#!/bin/bash

# Script to test if we can read a keystore with the provided password
# This demonstrates that the password is used to ACCESS the keystore

echo "This script tests keystore access with password..."
echo ""

# Check if keystore exists
if [ ! -f "app/release-key.keystore" ]; then
    echo "❌ Keystore file not found at app/release-key.keystore"
    echo "Run ./generate-keystore.sh first to create one"
    exit 1
fi

# Check if keystore.properties exists
if [ ! -f "keystore.properties" ]; then
    echo "❌ keystore.properties file not found"
    echo "Copy keystore.properties.template to keystore.properties and fill in your passwords"
    exit 1
fi

# Read properties from file
source keystore.properties

echo "Testing keystore access..."
echo "Keystore file: app/release-key.keystore"
echo "Key alias: $KEY_ALIAS"
echo ""

# Try to list the keystore contents (this requires the password)
echo "Attempting to read keystore with provided password..."
keytool -list -keystore app/release-key.keystore -alias "$KEY_ALIAS" -storepass "$KEYSTORE_PASSWORD"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS: Keystore can be read with the provided password!"
    echo "✅ Your keystore.properties file has the correct password"
else
    echo ""
    echo "❌ FAILED: Cannot read keystore with the provided password"
    echo "❌ Check your KEYSTORE_PASSWORD in keystore.properties"
fi
