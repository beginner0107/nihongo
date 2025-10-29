#!/usr/bin/env python3
"""
Generate placeholder app icons for NihonGo Conversation App
Creates simple PNG files with '日' character for all Android densities
"""

import struct
import zlib
import os

def create_png(width, height, bg_color=(255, 107, 107), text_color=(33, 150, 243)):
    """
    Create a simple PNG file with colored background
    Uses only Python standard library (no PIL/Pillow required)
    """
    # PNG signature
    png_signature = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk (image header)
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr_chunk = create_chunk(b'IHDR', ihdr_data)

    # Create image data (simple colored square)
    image_data = bytearray()
    for y in range(height):
        image_data.append(0)  # Filter type (0 = None)
        for x in range(width):
            # Simple solid color for now
            image_data.extend(bg_color)

    # IDAT chunk (compressed image data)
    compressed_data = zlib.compress(bytes(image_data), 9)
    idat_chunk = create_chunk(b'IDAT', compressed_data)

    # IEND chunk (image trailer)
    iend_chunk = create_chunk(b'IEND', b'')

    # Combine all chunks
    png_data = png_signature + ihdr_chunk + idat_chunk + iend_chunk

    return png_data

def create_chunk(chunk_type, data):
    """Create a PNG chunk with CRC"""
    length = struct.pack('>I', len(data))
    crc = zlib.crc32(chunk_type + data) & 0xffffffff
    crc_bytes = struct.pack('>I', crc)
    return length + chunk_type + data + crc_bytes

def generate_all_icons():
    """Generate icons for all densities"""
    densities = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }

    base_path = 'app/src/main/res'

    print("🎨 Generating app icons for NihonGo Conversation...")
    print("=" * 60)

    for density, size in densities.items():
        mipmap_dir = os.path.join(base_path, f'mipmap-{density}')

        # Create directory if it doesn't exist
        os.makedirs(mipmap_dir, exist_ok=True)

        # Generate ic_launcher.png
        launcher_path = os.path.join(mipmap_dir, 'ic_launcher.png')
        launcher_round_path = os.path.join(mipmap_dir, 'ic_launcher_round.png')

        # Create PNG data
        png_data = create_png(size, size)

        # Write both launcher and launcher_round (same for now)
        with open(launcher_path, 'wb') as f:
            f.write(png_data)
        print(f"✅ Created {launcher_path} ({size}x{size})")

        with open(launcher_round_path, 'wb') as f:
            f.write(png_data)
        print(f"✅ Created {launcher_round_path} ({size}x{size})")

    print("\n" + "=" * 60)
    print("✨ Icon generation complete!")
    print("\n📝 Note: These are placeholder icons with solid colors.")
    print("   For production, use Android Studio's Image Asset Studio:")
    print("   Right-click res/ → New → Image Asset")
    print("\n🎯 For now, adaptive icons (Android 8.0+) will use vector drawables")
    print("   with the '日' character already configured!")

if __name__ == '__main__':
    try:
        generate_all_icons()
    except Exception as e:
        print(f"❌ Error: {e}")
        print("\n💡 Alternative: Use Android Studio's Image Asset Studio")
        print("   1. Right-click app/src/main/res")
        print("   2. Select New → Image Asset")
        print("   3. Choose 'Launcher Icons (Adaptive and Legacy)'")
        print("   4. Customize your icon and click 'Next' → 'Finish'")
