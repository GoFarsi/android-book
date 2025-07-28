#!/bin/bash

# Script to generate a release keystore for Android app signing
# This will create a release-key.keystore file in the app directory

echo "Generating Android release keystore..."
echo "Please provide the following information:"

read -p "Enter your full name: " FULL_NAME
read -p "Enter your organization unit (e.g., Development): " ORG_UNIT
read -p "Enter your organization (e.g., Your Company): " ORGANIZATION
read -p "Enter your city: " CITY
read -p "Enter your state/province: " STATE
read -p "Enter your country code (e.g., US, IR): " COUNTRY

echo ""
echo "Choose a key alias (this will be used to identify your key):"
read -p "Key alias: " KEY_ALIAS

echo ""
echo "The keystore and key passwords should be strong and secure."
echo "You will need these passwords every time you sign your app."

# Generate the keystore
keytool -genkey -v -keystore app/release-key.keystore -alias "$KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=$FULL_NAME, OU=$ORG_UNIT, O=$ORGANIZATION, L=$CITY, ST=$STATE, C=$COUNTRY"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore generated successfully!"
    echo "📁 Location: app/release-key.keystore"
    echo ""
    echo "Now create a keystore.properties file with your credentials:"
    echo "1. Copy keystore.properties.template to keystore.properties"
    echo "2. Fill in your actual passwords and key alias"
    echo "3. Never commit keystore.properties to version control"
    echo ""
    echo "To build a release APK, run: ./gradlew assembleRelease"
else
    echo "❌ Failed to generate keystore"
    exit 1
fi
