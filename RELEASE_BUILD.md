# Release Build Instructions

This document explains how to set up and build release versions of your Android app with proper signing.

## Initial Setup (One-time)

### 1. Generate a Release Keystore

Run the keystore generation script:
```bash
./generate-keystore.sh
```

This will:
- Create a `app/release-key.keystore` file
- Prompt you for certificate information (name, organization, etc.)
- **Prompt you to create strong passwords** for both keystore and key
- Generate a secure keystore for signing your releases

**Important**: You'll be asked to create two passwords:
1. **Keystore password**: Protects the entire keystore file
2. **Key password**: Protects your specific signing key

Choose strong, unique passwords and store them securely!

### 2. Configure Keystore Properties

1. Copy the template file:
   ```bash
   cp keystore.properties.template keystore.properties
   ```

2. Edit `keystore.properties` and fill in your actual values:
   ```properties
   KEYSTORE_PASSWORD=your_actual_keystore_password
   KEY_ALIAS=your_actual_key_alias
   KEY_PASSWORD=your_actual_key_password
   ```

   **Example**:
   ```properties
   KEYSTORE_PASSWORD=MySecureKeystorePass123!
   KEY_ALIAS=my-release-key
   KEY_PASSWORD=MySecureKeyPass456!
   ```

3. **Important**: Never commit `keystore.properties` to version control!

## Building Release APK

Once setup is complete, build your release APK:

```bash
./gradlew assembleRelease
```

The signed APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

## Building Release AAB (for Google Play)

For Google Play Store uploads, build an Android App Bundle:

```bash
./gradlane bundleRelease
```

The AAB will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

## Security Notes

- Keep your keystore file (`app/release-key.keystore`) secure
- Keep your `keystore.properties` file secure and never commit it
- Use strong passwords for both keystore and key
- Back up your keystore file - if you lose it, you cannot update your published app

## Troubleshooting

### Missing keystore.properties
If you get build errors about missing keystore properties:
1. Ensure `keystore.properties` exists in the root directory
2. Verify all required properties are set
3. Check that the keystore file path is correct

### Keystore not found
If the build fails with "keystore not found":
1. Ensure `app/release-key.keystore` exists
2. Re-run `./generate-keystore.sh` if needed
