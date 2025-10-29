# App Icon Setup - NihonGo Conversation

## 📱 Current Icon Configuration

Your app now has a complete icon system with:

✅ **Adaptive Icons** (Android 8.0+) - Vector-based with '日' character
✅ **Legacy PNG Icons** (Android 7.1 and below) - All densities covered
✅ **Proper AndroidManifest.xml** configuration

---

## 🎨 What's Included

### Adaptive Icons (Android 8.0+)
Located in `app/src/main/res/mipmap-anydpi-v26/`:
- `ic_launcher.xml` - Standard launcher icon
- `ic_launcher_round.xml` - Round launcher icon

**Design:**
- **Background**: Red gradient (`#FF6B6B` → `#FF5252`)
- **Foreground**: White circle with blue '日' (Japanese "sun/day") character
- **Supports**: All adaptive icon shapes (circle, squircle, rounded square, etc.)

### Legacy PNG Icons
Located in `app/src/main/res/mipmap-{density}/`:

| Density | Size | Files |
|---------|------|-------|
| mdpi | 48x48 | ic_launcher.png, ic_launcher_round.png |
| hdpi | 72x72 | ic_launcher.png, ic_launcher_round.png |
| xhdpi | 96x96 | ic_launcher.png, ic_launcher_round.png |
| xxhdpi | 144x144 | ic_launcher.png, ic_launcher_round.png |
| xxxhdpi | 192x192 | ic_launcher.png, ic_launcher_round.png |

**Note:** Current PNGs are simple placeholders with solid red background.

### Vector Drawables
Located in `app/src/main/res/drawable/`:
- `ic_launcher_background.xml` - Gradient background layer
- `ic_launcher_foreground.xml` - '日' character with white circle

---

## 🔧 Regenerating Icons

If you want to recreate the placeholder PNGs:

```bash
# From project root directory
python3 generate_icons.py
```

This will regenerate all PNG files for all densities.

---

## 🎨 Creating Custom Icons (Recommended for Production)

### Option 1: Android Studio Image Asset Studio (Easiest)

1. **Open Android Studio**
2. **Right-click** `app/src/main/res` in the Project panel
3. **Select** `New → Image Asset`
4. **Choose** "Launcher Icons (Adaptive and Legacy)"
5. **Configure your icon:**
   - Foreground Layer: Upload image or use clipart
   - Background Layer: Choose color or image
   - Preview all shapes and densities
6. **Click** `Next` → `Finish`

This will automatically:
- Generate all PNG densities
- Create adaptive icon XMLs
- Update mipmap folders
- Preserve vector drawables

### Option 2: Manual Design

**Design Specifications:**

**Adaptive Icon (Android 8.0+):**
- Canvas: 108x108 dp
- Safe zone: 72x72 dp (center)
- Trim zone: 18 dp margin on all sides
- Formats: XML Vector Drawable or PNG

**Legacy Icon:**
- Sizes: 48, 72, 96, 144, 192 dp
- Format: PNG (24-bit RGB or 32-bit RGBA)
- Background: Opaque (not transparent)

**Design Guidelines:**
- Keep important content in center 66% of icon
- Avoid text unless essential
- Use recognizable imagery
- Test on multiple device shapes

**Tools:**
- Figma, Sketch, Adobe XD
- GIMP, Photoshop
- Online: https://romannurik.github.io/AndroidAssetStudio/

### Option 3: Edit Existing Vector Drawables

**Modify the '日' character design:**

Edit `app/src/main/res/drawable/ic_launcher_foreground.xml`:

```xml
<!-- Change colors -->
<path
    android:fillColor="#YOUR_COLOR"
    android:strokeColor="#YOUR_STROKE"
    .../>

<!-- Adjust character size -->
<group
    android:scaleX="0.8"  <!-- Change scale -->
    android:scaleY="0.8">
    <!-- Your paths -->
</group>
```

**Modify background:**

Edit `app/src/main/res/drawable/ic_launcher_background.xml`:

```xml
<path
    android:fillColor="#YOUR_BG_COLOR"
    .../>
```

After editing, rebuild the app to see changes on Android 8.0+ devices.

---

## 🧪 Testing Your Icons

### Preview in Android Studio

1. Open `ic_launcher.xml` in Android Studio
2. See preview in Design view
3. Switch between device shapes

### Test on Emulator/Device

```bash
# Install app
./gradlew installDebug

# Check icon on:
# - Home screen
# - App drawer
# - Recent apps
# - Settings → Apps
```

### Test Different Android Versions

- **Android 7.1 and below**: Uses PNG from mipmap folders
- **Android 8.0+**: Uses adaptive icon (XML)
- **Android 12+**: May apply Material You theming

---

## 📂 File Structure

```
app/src/main/res/
├── drawable/
│   ├── ic_launcher_background.xml   # Adaptive icon background
│   └── ic_launcher_foreground.xml   # Adaptive icon foreground
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml              # Adaptive icon config
│   └── ic_launcher_round.xml        # Round adaptive icon config
├── mipmap-mdpi/
│   ├── ic_launcher.png              # 48x48
│   └── ic_launcher_round.png        # 48x48
├── mipmap-hdpi/
│   ├── ic_launcher.png              # 72x72
│   └── ic_launcher_round.png        # 72x72
├── mipmap-xhdpi/
│   ├── ic_launcher.png              # 96x96
│   └── ic_launcher_round.png        # 96x96
├── mipmap-xxhdpi/
│   ├── ic_launcher.png              # 144x144
│   └── ic_launcher_round.png        # 144x144
└── mipmap-xxxhdpi/
    ├── ic_launcher.png              # 192x192
    └── ic_launcher_round.png        # 192x192
```

---

## 🎯 AndroidManifest.xml Configuration

The manifest is already properly configured:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>
```

**How it works:**
1. Android 8.0+ → Uses `mipmap-anydpi-v26/ic_launcher.xml` (adaptive)
2. Android 7.1- → Uses `mipmap-{density}/ic_launcher.png` (legacy)
3. System automatically selects appropriate density

---

## 🚀 Quick Commands

```bash
# Regenerate placeholder PNGs
python3 generate_icons.py

# View generated icons
ls -lh app/src/main/res/mipmap-*/ic_launcher*.png

# Clean and rebuild with new icons
./gradlew clean
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

---

## 💡 Tips

1. **Keep it Simple**: Icons should be recognizable at small sizes
2. **Test on Multiple Devices**: Different OEMs may display icons differently
3. **Material Design**: Follow [Material icon design guidelines](https://material.io/design/iconography)
4. **Adaptive Icon Layers**: Keep foreground and background separate for animation effects
5. **Avoid Text**: Small text is hard to read at icon sizes
6. **Use Vector**: Vector drawables scale perfectly for adaptive icons

---

## 📚 Resources

- [Android Icon Design Guidelines](https://developer.android.com/distribute/google-play/resources/icon-design-specifications)
- [Adaptive Icons](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive)
- [Material Design Icons](https://material.io/design/iconography)
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/)
- [Icon Shapes](https://developer.android.com/about/versions/oreo/android-8.0#o-icons)

---

## ❓ Troubleshooting

### Icon not showing after installation
```bash
# Clear app data and reinstall
adb uninstall com.nihongo.conversation
./gradlew installDebug
```

### Icon looks different on different devices
- This is normal! Adaptive icons adapt to device shape
- Test on multiple device shapes in emulator

### PNG icons not generating
```bash
# Check Python is installed
python3 --version

# Run generation script with verbose output
python3 generate_icons.py
```

### Adaptive icon not showing on Android 8.0+
- Check `mipmap-anydpi-v26` folder exists
- Verify `ic_launcher.xml` references correct drawables
- Ensure drawable XMLs are valid

---

## 🎨 Current Design Philosophy

The '日' (sun/day) character represents:
- ☀️ **Japanese language** (日本語 - nihongo)
- 📅 **Daily practice** - learning every day
- 🌅 **New beginnings** - each conversation is a new opportunity

**Colors:**
- 🔴 **Red background** (#FF6B6B) - Energy and passion
- 🔵 **Blue character** (#2196F3) - Trust and learning
- ⚪ **White circle** - Clarity and focus

Feel free to customize these to match your brand!
