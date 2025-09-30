# Firebase Setup Instructions

## Initial Setup

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or select existing project
   - Note down your Project ID

2. **Add Android App to Firebase**
   - Click "Add app" and select Android
   - Enter package name: `com.app.zecara`
   - Download the `google-services.json` file

3. **Configure App**
   - Place the `google-services.json` file in `app/` directory
   - The file should never be committed to version control

## Firebase Services Required

### Authentication
- Enable Google Sign-In in Authentication > Sign-in method
- Add your app's SHA-1 fingerprint in Project Settings

### Firestore Database
- Create a Firestore database in production mode
- Set up security rules for your content structure

### Storage
- Enable Cloud Storage
- Configure storage rules for file uploads

## Security Notes

- **NEVER** commit `google-services.json` to Git
- Use the template file (`google-services.json.template`) as a reference
- Each developer needs their own Firebase configuration
- API keys in this file are public but restricted by package name and SHA fingerprint

## Getting SHA-1 Fingerprint

For debug builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

For release builds:
```bash
keytool -list -v -keystore path/to/your/keystore -alias your-key-alias
```
