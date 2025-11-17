# Google AI Edge Gallery - AI Coding Assistant Instructions

## Project Overview

This is an Android app for running generative AI models locally on-device using Google AI Edge technologies. The app provides multiple AI tasks (chat, prompt lab, image analysis, audio transcription) with downloadable LiteRT models from HuggingFace.

## Architecture Patterns

### Core Components
- **Task System**: `data/Tasks.kt` defines available AI tasks. Each task has categories, associated models, and UI handlers
- **Model Management**: `data/Model.kt` contains model metadata, download info, and configuration. Models are stored in `model_allowlist.json`
- **Download Pipeline**: `data/DownloadRepository.kt` handles HuggingFace OAuth and model downloads
- **Data Persistence**: Uses DataStore with Protocol Buffers for settings (`proto/settings.proto`) and user data

### Key Architecture Decisions
- **Hilt Dependency Injection**: All repositories and services are injected via `di/AppModule.kt`
- **Jetpack Compose UI**: Modern declarative UI with Material3 theming in `ui/theme/`
- **Task-Model Relationship**: Tasks define what users can do; models provide the AI capabilities for those tasks
- **Custom Task Extensions**: Framework in `customtasks/` allows adding new AI tasks without core changes

## Development Setup

### Required Configuration
1. **HuggingFace OAuth**: Must configure in `common/ProjectConfig.kt`:
   - Set `clientId` and `redirectUri` from your HuggingFace developer app
   - Update `manifestPlaceholders["appAuthRedirectScheme"]` in `app/build.gradle.kts`

### Build Commands
```bash
cd Android/src
./gradlew assembleDebug  # Build debug APK
./gradlew installDebug   # Install to connected device
```

### Key Build Features
- **Min SDK 31** (Android 12+) for NPU support
- **Context receivers** enabled for Kotlin
- **Protocol Buffers** for efficient data serialization
- **LiteRT integration** for on-device AI inference

## Code Patterns

### Model Integration
When adding new models, update `model_allowlist.json` with:
- Model metadata, download URLs, and task type compatibility
- Memory estimates and default inference configurations
- Version tracking for model updates

### Task Implementation
New AI tasks follow this pattern:
1. Define task in `data/Tasks.kt` with category and models
2. Create UI components in `ui/{task_name}/`
3. Implement ViewModels with inference logic
4. Register navigation routes in `ui/navigation/`

### Data Flow
- **Repository Pattern**: DataStore and Download repositories abstract data access
- **State Management**: ViewModels use Compose state for UI updates
- **Background Processing**: WorkManager handles model downloads and inference

### File Organization
- `common/`: Shared utilities and configuration
- `data/`: Data models, repositories, and business logic
- `ui/`: Compose UI components organized by feature
- `worker/`: Background tasks for downloads and processing

## Testing & Debugging

### Model Testing
- Use "Bring Your Own Model" feature to test custom `.litertlm` files
- Performance metrics (TTFT, decode speed) are tracked in real-time
- Model inference happens on background threads to maintain UI responsiveness

### Common Issues
- **NPU Availability**: App gracefully falls back to GPU/CPU when NPU unavailable
- **Memory Management**: Large models require careful memory monitoring
- **Download Failures**: Robust retry logic handles network interruptions

## Integration Points

### External Dependencies
- **LiteRT**: Core inference runtime (`libs.litertlm`)
- **HuggingFace**: Model discovery and OAuth authentication
- **Firebase**: Analytics and crash reporting (optional)
- **CameraX**: Image capture for vision tasks

### Platform Integration
- **Android 12+ Features**: Leverages NPU acceleration when available
- **Material You**: Dynamic theming support
- **Edge-to-Edge**: Modern Android UI patterns
- **Splash Screen**: Custom animated splash screen with model loading

## Security Considerations
- OAuth tokens stored in encrypted SharedPreferences
- Model files validated before inference
- Network requests use secure HTTPS endpoints
- No sensitive data transmitted to external services during inference