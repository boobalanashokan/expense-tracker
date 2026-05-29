# Kanakku Android App + Widget

WebView app + home screen widget for the Kanakku expense tracker.
Builds entirely in GitHub Actions — no local Android Studio needed.

---

## ⚡ One-time setup (5 minutes)

### Step 1 — Copy files into your repo

Copy the `kanakku-android/` folder into the **root** of your existing GitHub repo.
Your repo structure should look like:

```
your-repo/
├── kanakku.html          ← your existing web app
├── kanakku-android/      ← new folder from this zip
│   ├── .github/
│   │   └── workflows/
│   │       └── build.yml
│   ├── app/
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradlew
└── ...
```

### Step 2 — Set your app URL

Open `kanakku-android/app/src/main/java/com/kanakku/app/MainActivity.java`

Change this line to your actual GitHub Pages URL:
```java
public static final String APP_URL = "https://YOUR_USERNAME.github.io/YOUR_REPO/kanakku.html";
```

For example:
```java
public static final String APP_URL = "https://rajan.github.io/kanakku/kanakku.html";
```

### Step 3 — Enable GitHub Pages (if not already)

Go to your repo → **Settings** → **Pages** → Source: `main` branch → `/ (root)`

This gives you the URL above.

### Step 4 — Push to GitHub

```bash
git add .
git commit -m "Add Android app and widget"
git push
```

GitHub Actions will automatically start building. Takes ~5 minutes.

### Step 5 — Download APK

1. Go to your repo on GitHub
2. Click **Actions** tab
3. Click the latest workflow run
4. Scroll down to **Artifacts** → download `kanakku-debug-apk`
5. Unzip → you get `app-debug.apk`

OR check the **Releases** section — the APK is also uploaded there automatically.

### Step 6 — Install on Samsung

1. Transfer APK to your phone (WhatsApp to yourself, email, Google Drive, USB)
2. Open the APK file on your phone
3. Samsung will ask to enable **"Install unknown apps"** — allow it once
4. Install → done!

---

## 📱 How the widget works

**After installing the app:**
1. Long-press your home screen
2. Tap **Widgets**
3. Find **Kanakku** → you'll see two options:
   - **Kanakku (2×2)** — small widget: monthly total + today + add/refund buttons
   - **Kanakku Dashboard (4×2)** — wide widget: stats + last 3 transactions + add/refund buttons
4. Drag to place on home screen

**First time:** Open the Kanakku app and sign in first. The widget reads your email from the app session to fetch data.

**Widget buttons:**
- **➕ Add** → opens a quick-add dialog
- **↩ Refund** → opens quick-add in refund mode
- **⟳** (refresh icon) → manually refreshes data
- Tap anywhere else → opens the full app

**Auto-refresh:** Every 30 minutes automatically.

---

## 🔁 Updating the app

Every time you push changes to `main`, GitHub Actions rebuilds the APK.
Download the new APK from Actions → Artifacts and reinstall on your phone.

---

## ❓ Troubleshooting

**Widget shows "—" for amounts:**
Open the Kanakku app, sign in, then tap the ⟳ refresh button on the widget.

**"Install unknown apps" not showing:**
Samsung → Settings → Apps → Special app access → Install unknown apps → Files/Browser → Allow

**Build failing in GitHub Actions:**
Check the Actions tab for error details. Usually it's a wrong APP_URL or missing gradlew permissions.
