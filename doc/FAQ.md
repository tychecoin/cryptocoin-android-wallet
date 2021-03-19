# FAQ

## What is it?
- A **Cryptocoin** lightwallet for Android. You get to choose what remote node you want to connect to. No need to download the blockchain.

## What features does it have?

- Uses Cryptocoin v3.0.2
- Support for Android >= 5.0
- Mainnet
- Recover wallets from mnemonic seed or from keys
- *Street Mode* which will hide the CRYPTO amount on the screen for security purposes
- Multiple wallets
- Support for accounts
- View wallet details (address, keys, etc.)
- View transactions including details and copy to clipboard
- Spend CRYPTO (handle with care on mainnet!)
- Manually import existing wallet (by copying them to the Cryptocoin folder)
- Background updating (make sure you exit the wallet to stop updating to save some battery)
- Access to daemon with username/password and nonstandard port
- Only 5 decimal places shown in transactions (full amount in details - click on transaction)
- All significant figures shown in balance
- QR Code scanning - make sure to *ALWAYS* verify the scanned code is what it is advertised to be!
- QR Code for receiving with conversion of CRYPTO to USD/EUR and back through Coingecko API
- Backup wallets to `backups` folder in main wallet folder (old backups are overwritten)
- Rename wallets
- Archive (=Backup and delete)
- 1 default proxy remote node: *node1.crypt-o-coin.cash:22222*; you can check the individual remote node via https://node1.crypt-o-coin.cash
- Node-o-matic engine will scan 10 available public remote nodes on Internet and automatically bookmark the best node for you

## How do I use a node with username & password?
```username:password@node.address:portnumber```

## The app always crashes as soon as I open a wallet!
Users of Zenfone MAX & Zenfone 2 Laser (possibly others) **MUST** use the armeabi-v7a APK as the arm64-v8a build uses hardware AES
functionality these models don't have. If this does not help, please report the problem.

## I cannot select and copy the mnemonic seed
Copying anything to the clipboard on Android exposes it to any other App running. So this
is a security measure to keep your seed safe(r). 

## My storage is getting full
Newly generated wallets are stored in `.new` in the main wallet folder.
They are never erased (for now). You can delete this whole folder from time to time.

Also, the backup folder named "`backups`" (formerly `.backups`) is never automatically cleaned up.
You may want to do housekeeping manually with a file browser.

All wallet files (`testnet` and `mainnet`) are stored in the main `Cryptocoin Wallet` folder.
So be careful erasing stuff. One of the future releases will split the wallets and move `testnet`
 wallets out of there.

## Do you have any screenshots of what it looks like and how it works?
No, but it looks fantastic. Just check it out.

## Can I use existing wallet files?

If you want to use existing wallet files, you need to copy the wallet files from you current Cryptocoin client. These are:
```
WalletName
WalletName.address.txt
WalletName.keys
```
