## Introduction

This is the code repository for the article `From Silence to Immersion: TouchSound's Tactile Rhythm and Visual Augmentation for DHH short-video viewers`. It includes the source code, released apk, and detailed documentation of visual effects (`emotion_effects.md`) corresponding to 8 different emotions.

## Supplementary Materials

### Visual Effects Documentation

[emotion_effects.md](./emotion_effects.md): Detailed explanation of visual effects corresponding to 8 different emotions


### Installation Package

The APK can be found in the [apk directory](./apk/).


## Preparation Before Running the Program

1. Extract the `src.zip` file to get the source code directory.
2. Use the latest version of Android Studio to open the `src` directory. 

This application uses external LLM (Large Language Model) API services that require configuration. Follow these steps to configure your LLM API settings:

1. Open the file `src/app/src/main/assets/api_keys.properties`
2. Replace the placeholder values with your actual LLM API settings:

```properties
# API Key for your LLM service
LLM_API_KEY=your_api_key

# LLM model name to use
LLM_MODEL_NAME=your_model_name

# LLM API endpoint URL
LLM_API_URL=your_api_endpoint_url
```

> **Important Note:**
> - System Requirements: Android 10 (API level 29) or above.
> - Testing in Android Studio is not convenient as it requires playing videos in a short-video app, which Android Studio doesn't provide.
> - We STRONGLY recommend using the APK we provided and testing it on Android phones (excluding Huawei devices). The provided APK has already been configured with proper LLM settings and is ready to use.


## Running the Program

Now you can run the program on your Android device:

1. Grant the required permissions as prompted.
2. Click the "Start Capture" button to initiate the service.
3. Adjust the music response parameters on the interface as needed:
   - These parameter changes will be directly reflected in the visual effects.
   - It's recommended to increase the "Overall Speed" parameter to obtain significant visual effects.
4. Open a short video application (such as TikTok) for testing.
5. Click the floating button to refresh the emotion analysis.