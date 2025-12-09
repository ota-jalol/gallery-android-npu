#!/bin/bash
# Merge all NPU runtime libraries into single arm64-v8a folder
# Libraries are renamed with version prefix to avoid conflicts
# Run this script from the jniLibs directory

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/arm64-v8a"

# Clean and create output directory
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "Merging NPU runtime libraries..."

# Copy Qualcomm libraries with version prefix
for version in 69 73 75 79; do
    SRC_DIR="$SCRIPT_DIR/qualcomm_runtime_v${version}/src/main/jni/arm64-v8a"
    if [ -d "$SRC_DIR" ]; then
        echo "Processing Qualcomm v${version}..."
        for so_file in "$SRC_DIR"/*.so; do
            if [ -f "$so_file" ]; then
                filename=$(basename "$so_file")
                # Rename with version prefix: libQnnSystem.so -> libQnnSystemV69.so
                # But keep version-specific files as-is (already have version in name)
                if [[ "$filename" == *"V${version}"* ]]; then
                    # Already has version in name (e.g., libQnnHtpV73Stub.so)
                    new_name="$filename"
                else
                    # Add version suffix before .so (e.g., libQnnSystem.so -> libQnnSystemV69.so)
                    new_name="${filename%.so}V${version}.so"
                fi
                cp "$so_file" "$OUTPUT_DIR/$new_name"
                echo "  - $filename -> $new_name"
            fi
        done
    fi
done

# Copy Google Tensor libraries
if [ -d "$SCRIPT_DIR/google_tensor_runtime/src/main/jni/arm64-v8a" ]; then
    echo "Processing Google Tensor..."
    for so_file in "$SCRIPT_DIR/google_tensor_runtime/src/main/jni/arm64-v8a"/*.so; do
        if [ -f "$so_file" ]; then
            filename=$(basename "$so_file")
            cp "$so_file" "$OUTPUT_DIR/$filename"
            echo "  - $filename"
        fi
    done
fi

# Copy MediaTek libraries
if [ -d "$SCRIPT_DIR/mediatek_runtime/src/main/jni/arm64-v8a" ]; then
    echo "Processing MediaTek..."
    for so_file in "$SCRIPT_DIR/mediatek_runtime/src/main/jni/arm64-v8a"/*.so; do
        if [ -f "$so_file" ]; then
            filename=$(basename "$so_file")
            cp "$so_file" "$OUTPUT_DIR/$filename"
            echo "  - $filename"
        fi
    done
fi

echo ""
echo "Merged libraries in $OUTPUT_DIR:"
ls -la "$OUTPUT_DIR/"
echo ""
echo "Done! Total files: $(ls -1 "$OUTPUT_DIR/" | wc -l)"
