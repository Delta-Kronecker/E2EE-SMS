# E2EE-SMS

End-to-End Encrypted SMS Messenger for Android.

## Features

- **End-to-End Encryption** — Messages are encrypted with AES-256-GCM. Only the recipient can read them.
- **No Server Required** — All communication happens directly via SMS. No internet needed.
- **Pairing System** — Share a pairing string to establish encrypted communication with contacts.
- **Delivery Reports** — Track message status: Sent, Delivered, or Failed.
- **Dark Theme** — Modern dark UI with purple accent.
- **Phone Number Integration** — Country code selector with 20+ countries (default: Iran +98).

## How It Works

### 1. Setup
- Install the app
- Enter your display name and phone number
- Save the backup phrase (12 words) — this is the ONLY way to recover your account

### 2. Pairing
- Go to **Pair** tab to see your pairing string
- Share it with your contact (copy & send via any messaging app)
- Your contact imports your pairing string in the **Import** tab
- Both sides can now communicate with E2EE

### 3. Messaging
- Select a contact from the conversation list
- Type and send messages — they are automatically encrypted
- Messages show delivery status: ⏳ (pending) → ✓ (sent) → ✓✓ (delivered)

## Pairing String Format

```
E2EE-SMS:2:{uuid}:{name}:{phone}:{publicKey}
```

| Field | Description |
|-------|-------------|
| Version | Protocol version (currently 2) |
| UUID | Unique identifier for the user |
| Name | Display name |
| Phone | Full phone number with country code |
| Public Key | EC public key for key agreement |

## Cryptography

| Algorithm | Purpose |
|-----------|---------|
| **ECDH (secp256r1)** | Key agreement — derives shared secret from key pairs |
| **AES-256-GCM** | Message encryption — authenticated encryption with integrity |
| **HMAC-SHA256** | Key derivation — derives message encryption key from shared secret |
| **EncryptedSharedPreferences** | Key storage — keys stored encrypted on device |

### Flow

1. **Pairing**: Both users exchange EC public keys via pairing string
2. **Shared Secret**: Each user computes `ECDH(my_private, other_public)` to get a shared secret
3. **Encryption**: Before sending, message is encrypted with AES-256-GCM using a key derived from the shared secret
4. **Decryption**: Receiver derives the same key and decrypts the message

## Permissions

| Permission | Purpose |
|------------|---------|
| `SEND_SMS` | Send encrypted messages |
| `RECEIVE_SMS` | Receive and decrypt incoming messages |
| `READ_SMS` | Read SMS history for conversation display |
| `READ_CONTACTS` | (Optional) Contact name lookup |

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Release

To create a release:

1. Push a tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. GitHub Actions will automatically:
   - Build the release APK
   - Create a GitHub Release
   - Upload the APK as a release asset

## Project Structure

```
app/src/main/java/com/example/sms/
├── crypto/
│   ├── KeyManager.kt        # Key generation, storage, ECDH agreement
│   ├── CryptoEngine.kt      # AES-256-GCM encrypt/decrypt
│   └── PairingManager.kt    # Pairing string creation/parsing
├── db/
│   ├── AppDatabase.kt       # Room database
│   ├── ContactDao.kt        # Contact queries
│   └── MessageDao.kt        # Message queries
├── model/
│   ├── Contact.kt           # Contact data class
│   └── Message.kt           # Message data class
├── SetupActivity.kt         # First-run setup + backup phrase
├── PairActivity.kt          # Pairing string display/import
├── MainActivity.kt          # Conversation list
├── ChatActivity.kt          # Chat with encryption/decryption
├── ChatMessageAdapter.kt    # Chat message adapter (sent/received)
├── ContactAdapter.kt        # Conversation list adapter
└── SmsReceiver.kt           # Incoming SMS handler
```

## Security Notes

- Messages are encrypted end-to-end — no one else can read them
- The app does NOT use any server — all data stays on your device
- Backup phrase is the only way to recover your account
- Shared secrets are computed using ECDH and stored encrypted
- Each message uses a fresh encryption key derived from the shared secret

## License

MIT
