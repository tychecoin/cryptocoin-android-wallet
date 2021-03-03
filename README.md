# Cryptocoin Wallet
Android Wallet for $CRYPTO

### QUICKSTART
- Download the APK for the most current release [here](https://github.com/gonzothedev/crypto-android-wallet/releases/latest) and install it
- Run the App and tap + button to Create new wallet or restore from 25 word seeds
- Advanced users can copy over synced wallet files (all files) onto sdcard in directory Cryptocoin Wallet (created first time App is started)
- See the [FAQ](doc/FAQ.md)

### Disclaimer
You may lose all your $CRYPTO if you use this App. Be cautious when spending on the mainnet.

### Random Notes
- works on the mainnet
- use your own daemon - it's easy
- Cryptocoin Wallet is the name of this Android Wallet for Cryptocoin

### Issues / Pitfalls
- Users of Zenfone MAX & Zenfone 2 Laser (possibly others) **MUST** use the armeabi-v7a APK as the arm64-v8a build uses hardware AES
functionality these models don't have.
- You should backup your wallet files in the "monerujo" folder periodically.
- Also note, that on some devices the backups will only be visible on a PC over USB after a reboot of the device (it's an Android bug/feature)
- Created wallets on a private testnet are unusable because the restore height is set to that
of the "real" testnet.  After creating a new wallet, make a **new** one by recovering from the seed.
The official cryptocoin client shows the same behaviour.

### HOW TO BUILD

See [the instructions](doc/BUILDING-external-libs.md)

Then, fire up Android Studio and build the APK.

### Donations
- Address: fh2jc6PbQYd4a5PY3ooPMZiPVniMy4MGcjSRBnoBVc1xLmdCHJ6hc98Ess2hpN2mDgPnCAXtDUUbmjWYutRvdoSr2Nps2o5wc
- Viewkey: 84a6ac951dccea8aa5c23127f9e69bd71222d64c0b560bbec49a6e6c01df1205
