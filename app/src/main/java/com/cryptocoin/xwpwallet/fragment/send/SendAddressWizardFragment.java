/*
 * Copyright (c) 2017 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cryptocoin.cryptowallet.fragment.send;

import android.content.Context;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cryptocoin.cryptowallet.R;
import com.cryptocoin.cryptowallet.data.BarcodeData;
import com.cryptocoin.cryptowallet.data.TxData;
import com.cryptocoin.cryptowallet.data.TxDataBtc;
import com.cryptocoin.cryptowallet.data.UserNotes;
import com.cryptocoin.cryptowallet.model.PendingTransaction;
import com.cryptocoin.cryptowallet.model.Wallet;
import com.cryptocoin.cryptowallet.util.BitcoinAddressValidator;
import com.cryptocoin.cryptowallet.util.Helper;
import com.cryptocoin.cryptowallet.util.OpenAliasHelper;
import com.cryptocoin.cryptowallet.util.PaymentProtocolHelper;
import com.cryptocoin.cryptowallet.xmrto.XmrToError;
import com.cryptocoin.cryptowallet.xmrto.XmrToException;

import java.util.Map;

import timber.log.Timber;

public class SendAddressWizardFragment extends SendWizardFragment {

    static final int INTEGRATED_ADDRESS_LENGTH = 108;

    public static SendAddressWizardFragment newInstance(Listener listener) {
        SendAddressWizardFragment instance = new SendAddressWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendAddressWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    public interface Listener {
        void setBarcodeData(BarcodeData data);

        BarcodeData getBarcodeData();

        BarcodeData popBarcodeData();

        void setMode(SendFragment.Mode mode);

        TxData getTxData();
    }

    private EditText etDummy;
    private TextInputLayout etAddress;
    private TextInputLayout etNotes;
    private CardView cvScan;
    private View tvPaymentIdIntegrated;
    private ImageButton bPasteAddress;

    private boolean resolvingOA = false;
    private boolean resolvingPP = false;
    private String resolvedPP = null;

    OnScanListener onScanListener;

    public interface OnScanListener {
        void onScan();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(R.layout.fragment_send_address, container, false);

        tvPaymentIdIntegrated = view.findViewById(R.id.tvPaymentIdIntegrated);

        etAddress = view.findViewById(R.id.etAddress);
        etAddress.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etAddress.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // ignore ENTER
                return ((event != null) && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER));
            }
        });
        etAddress.getEditText().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    View next = etAddress;
                    String enteredAddress = etAddress.getEditText().getText().toString().trim();
                    String dnsOA = dnsFromOpenAlias(enteredAddress);
                    Timber.d("OpenAlias is %s", dnsOA);
                    if (dnsOA != null) {
                        processOpenAlias(dnsOA);
                        next = null;
                    } else {
                        // maybe a bip72 or 70 URI
                        String bip70 = PaymentProtocolHelper.getBip70(enteredAddress);
                        if (bip70 != null) {
                            // looks good - resolve through xmr.to
                            processBip70(bip70);
                        }
                    }
                }
            }
        });
        etAddress.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                Timber.d("AFTER: %s", editable.toString());
                if (editable.toString().equals(resolvedPP)) return; // no change required
                resolvedPP = null;
                etAddress.setError(null);
                if (isIntegratedAddress()) {
                    Timber.d("isIntegratedAddress");
                    etAddress.setError(getString(R.string.info_paymentid_integrated));
                    sendListener.setMode(SendFragment.Mode.XMR);
                } else {
                    Timber.d("isStandardAddress or other");
                    tvPaymentIdIntegrated.setVisibility(View.INVISIBLE);
                    sendListener.setMode(SendFragment.Mode.XMR);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        bPasteAddress = view.findViewById(R.id.bPasteAddress);
        bPasteAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String clip = Helper.getClipBoardText(getActivity());
                if (clip == null) return;
                // clean it up
                final String address = clip.replaceAll("[^0-9A-Z-a-z]", "");
                if (Wallet.isAddressValid(address) || BitcoinAddressValidator.validate(address)) {
                    final EditText et = etAddress.getEditText();
                    et.setText(address);
                    et.setSelection(et.getText().length());
                    etAddress.requestFocus();
                } else
                    Toast.makeText(getActivity(), getString(R.string.send_address_invalid), Toast.LENGTH_SHORT).show();
            }
        });

        etNotes = view.findViewById(R.id.etNotes);
        etNotes.getEditText().setRawInputType(InputType.TYPE_CLASS_TEXT);
        etNotes.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                        || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    etDummy.requestFocus();
                    return true;
                }
                return false;
            }
        });

        cvScan = view.findViewById(R.id.bScan);
        cvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onScanListener.onScan();
            }
        });

        etDummy = view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.requestFocus();

        View tvNfc = view.findViewById(R.id.tvNfc);
        NfcManager manager = (NfcManager) getContext().getSystemService(Context.NFC_SERVICE);
        if ((manager != null) && (manager.getDefaultAdapter() != null))
            tvNfc.setVisibility(View.VISIBLE);

        return view;
    }

    private void processOpenAlias(String dnsOA) {
        if (resolvingOA) return; // already resolving - just wait
        sendListener.popBarcodeData();
        if (dnsOA != null) {
            resolvingOA = true;
            etAddress.setError(getString(R.string.send_address_resolve_openalias));
            OpenAliasHelper.resolve(dnsOA, new OpenAliasHelper.OnResolvedListener() {
                @Override
                public void onResolved(Map<BarcodeData.Asset, BarcodeData> dataMap) {
                    resolvingOA = false;
                    BarcodeData barcodeData = dataMap.get(BarcodeData.Asset.XMR);
                    if (barcodeData == null) barcodeData = dataMap.get(BarcodeData.Asset.BTC);
                    if (barcodeData != null) {
                        Timber.d("Security=%s, %s", barcodeData.security.toString(), barcodeData.address);
                        processScannedData(barcodeData);
                    } else {
                        etAddress.setError(getString(R.string.send_address_not_openalias));
                        Timber.d("NO XMR OPENALIAS TXT FOUND");
                    }
                }

                @Override
                public void onFailure() {
                    resolvingOA = false;
                    etAddress.setError(getString(R.string.send_address_not_openalias));
                    Timber.e("OA FAILED");
                }
            });
        } // else ignore
    }

    private void processBip70(final String bip70) {
        Timber.d("RESOLVED PP: %s", resolvedPP);
        if (resolvingPP) return; // already resolving - just wait
        resolvingPP = true;
        sendListener.popBarcodeData();
        etAddress.setError(getString(R.string.send_address_resolve_bip70));
        PaymentProtocolHelper.resolve(bip70, new PaymentProtocolHelper.OnResolvedListener() {
            @Override
            public void onResolved(BarcodeData.Asset asset, String address, double amount, String resolvedBip70) {
                resolvingPP = false;
                if (asset != BarcodeData.Asset.BTC)
                    throw new IllegalArgumentException("only BTC here");

                if (resolvedBip70 == null)
                    throw new IllegalArgumentException("success means we have a pp_url - else die");

                final BarcodeData barcodeData =
                        new BarcodeData(BarcodeData.Asset.BTC, address, null,
                                resolvedBip70, null, String.valueOf(amount),
                                BarcodeData.Security.BIP70);
                etNotes.post(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("security is %s", barcodeData.security);
                        processScannedData(barcodeData);
                        etNotes.requestFocus();
                    }
                });
            }

            @Override
            public void onFailure(final Exception ex) {
                resolvingPP = false;
                etAddress.post(new Runnable() {
                    @Override
                    public void run() {
                        int errorMsgId = R.string.send_address_not_bip70;
                        if (ex instanceof XmrToException) {
                            XmrToError error = ((XmrToException) ex).getError();
                            if (error != null) {
                                errorMsgId = error.getErrorMsgId();
                            }
                        }
                        etAddress.setError(getString(errorMsgId));
                    }
                });
                Timber.d("PP FAILED");
            }
        });
    }

    private boolean checkAddressNoError() {
        String address = etAddress.getEditText().getText().toString();
        return Wallet.isAddressValid(address)
                || BitcoinAddressValidator.validate(address)
                || (resolvedPP != null);
    }

    private boolean checkAddress() {
        boolean ok = checkAddressNoError();
        if (!ok) {
            etAddress.setError(getString(R.string.send_address_invalid));
        } else {
            etAddress.setError(null);
        }
        return ok;
    }

    private boolean isIntegratedAddress() {
        String address = etAddress.getEditText().getText().toString();
        return (address.length() == INTEGRATED_ADDRESS_LENGTH)
                && Wallet.isAddressValid(address);
    }

    private boolean isBitcoinAddress() {
        final String address = etAddress.getEditText().getText().toString();
        return BitcoinAddressValidator.validate(address);
    }

    private void shakeAddress() {
        etAddress.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    @Override
    public boolean onValidateFields() {
        if (!checkAddressNoError()) {
            shakeAddress();
            String enteredAddress = etAddress.getEditText().getText().toString().trim();
            String dnsOA = dnsFromOpenAlias(enteredAddress);
            Timber.d("OpenAlias is %s", dnsOA);
            if (dnsOA != null) {
                processOpenAlias(dnsOA);
            } else {
                String bip70 = PaymentProtocolHelper.getBip70(enteredAddress);
                if (bip70 != null) {
                    processBip70(bip70);
                }
            }
            return false;
        }

        if (sendListener != null) {
            TxData txData = sendListener.getTxData();
            if (txData instanceof TxDataBtc) {
                if (resolvedPP != null) {
                    // take the value from the field nonetheless as this is what the user sees
                    // (in case we have a bug somewhere)
                    ((TxDataBtc) txData).setBip70(etAddress.getEditText().getText().toString());
                    ((TxDataBtc) txData).setBtcAddress(null);
                } else {
                    ((TxDataBtc) txData).setBtcAddress(etAddress.getEditText().getText().toString());
                    ((TxDataBtc) txData).setBip70(null);
                }
                txData.setDestinationAddress(null);
            } else {
                txData.setDestinationAddress(etAddress.getEditText().getText().toString());
            }
            txData.setUserNotes(new UserNotes(etNotes.getEditText().getText().toString()));
            txData.setPriority(PendingTransaction.Priority.Priority_Default);
            txData.setMixin(SendFragment.MIXIN);
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScanListener) {
            onScanListener = (OnScanListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ScanListener");
        }
    }

    // QR Scan Stuff

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        processScannedData();
    }

    public void processScannedData(BarcodeData barcodeData) {
        sendListener.setBarcodeData(barcodeData);
        if (isResumed())
            processScannedData();
    }

    public void processScannedData() {
        resolvedPP = null;
        BarcodeData barcodeData = sendListener.getBarcodeData();
        if (barcodeData != null) {
            Timber.d("GOT DATA");

            if (barcodeData.address != null) {
                etAddress.getEditText().setText(barcodeData.address);
                if (checkAddress()) {
                    if (barcodeData.security == BarcodeData.Security.OA_NO_DNSSEC)
                        etAddress.setError(getString(R.string.send_address_no_dnssec));
                    else if (barcodeData.security == BarcodeData.Security.OA_DNSSEC)
                        etAddress.setError(getString(R.string.send_address_openalias));
                }
            } else {
                etAddress.getEditText().getText().clear();
                etAddress.setError(null);
            }

            String scannedNotes = barcodeData.description;
            if (scannedNotes != null) {
                etNotes.getEditText().setText(scannedNotes);
            } else {
                etNotes.getEditText().getText().clear();
                etNotes.setError(null);
            }
        } else
            Timber.d("barcodeData=null");
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        etDummy.requestFocus();
    }

    String dnsFromOpenAlias(String openalias) {
        Timber.d("checking openalias candidate %s", openalias);
        if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        if (Patterns.EMAIL_ADDRESS.matcher(openalias).matches()) {
            openalias = openalias.replaceFirst("@", ".");
            if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        }
        return null; // not an openalias
    }
}
