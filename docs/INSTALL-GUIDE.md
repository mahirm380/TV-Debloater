# Beginner installation guide

This guide takes you from a normal supported TV to the Apple TV-inspired home
screen in the screenshots. Nothing requires a USB cable when the TV supports ADB
over Wi-Fi. Allow 10–20 minutes and keep the TV and computer on the same private
network.

## Before you begin

You need:

- a Fire OS 6–8 television/stick, or an Android/Google TV running Android 7.1 or
  newer;
- a Mac, Windows or Linux computer on the same network;
- the TV remote in your hand for security prompts;
- Android's official `adb` platform tool;
- permission to manage the TV.

You do **not** need a USB drive, Ethernet cable, root access, an unlocked
bootloader, Android Studio or a factory reset.

If the TV runs Samsung Tizen, LG webOS, Roku, VIDAA or Philips Saphi, stop here.
Those systems cannot install this Android launcher. See [OTHER-TVS.md](OTHER-TVS.md)
for useful alternatives such as boot-to-HDMI and network DNS filtering.

## Choose how much to change

| Choice | Adds the launcher | Removes home-screen clutter | Disables telemetry packages | DNS filter | Reversible |
|---|---:|---:|---:|---:|---:|
| Launcher only | Yes | Yes, while using it | No | No | Yes |
| Full Fire OS setup | Yes | Yes | Yes | Optional | Yes |

Android/Google TV users should take the launcher-only route. The automated cleanup
scripts intentionally refuse non-Fire devices because vendor package names differ.

## Step 1: enable ADB on the TV

### Fire OS

1. Open **Settings → My Fire TV** or **Device & Software → About**.
2. Highlight the device name and press OK seven times.
3. Open **Developer Options** and turn **ADB debugging** on.
4. Find the address under **About → Network**. On some Fire TV Edition sets, open
   **Settings → Network** and select the connected Wi-Fi network.

### Android TV or Google TV

1. Open **Settings → System → About**.
2. Highlight **Build** and press OK seven times.
3. Open **Developer options**.
4. Turn on **USB debugging**, **Network debugging** or **Wireless debugging**—the
   label varies by brand and Android version.
5. Note the displayed connection address. Android 11+ may also show a separate
   pairing address and six-digit code.

Turn debugging off again when you are finished if you do not use it regularly.

## Step 2: install ADB on the computer

Download Android's official
[SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools),
extract it, and make the `adb` command available in Terminal/PowerShell. On macOS
with Homebrew, `brew install android-platform-tools` is another option.

Check it:

```sh
adb version
```

## Step 3: download this project

```sh
git clone https://github.com/mahirm380/TV-Debloater.git
cd TV-Debloater
```

If you prefer an AI to handle the remaining steps, open this folder in the coding
agent and paste the prompt from [AI-SETUP.md](AI-SETUP.md). The rest of this guide
is the manual route.

## Step 4: connect to the TV

For most Fire TVs and older Android TVs:

```sh
adb connect <TV-IP>:5555
adb devices
```

For a TV showing a pairing code:

```sh
adb pair <PAIR-IP>:<PAIR-PORT>
adb connect <CONNECT-IP>:<CONNECT-PORT>
adb devices
```

Replace the placeholders with the values shown on the TV. Approve **Allow USB
debugging?** with the remote and tick **Always allow from this computer**. Continue
only when `adb devices` shows exactly one intended TV ending in `device`.

`unauthorized` means the TV is waiting for approval. `offline` usually means the
TV needs a moment, or debugging should be toggled off and on. Do not keep retrying
while a prompt is waiting on the television.

## Step 5: install the launcher

```sh
./launcher/install.sh
```

The installer verifies the APK checksum before installing it. It then:

- uses the accessibility Home hand-off tested on Fire OS 7 and 8;
- uses the usage-access keeper tested on Fire OS 6; or
- requests the normal Android Home role on Android/Google TV.

It never disables a stock launcher on Android/Google TV. If Android shows a Home
app chooser, select **dumbtv home** and **Always**. If the vendor does not permit a
replacement Home app, open it from Apps instead.

## Step 6: set your weather

Ask an AI or a map service for your city's latitude, longitude and IANA timezone,
then run:

```sh
./launcher/configure.sh \
  --weather-name "Your city" \
  --latitude 00.0000 \
  --longitude 00.0000 \
  --timezone Area/City \
  --unit celsius
```

Use `--unit fahrenheit` where preferred, or hide weather:

```sh
./launcher/configure.sh --weather-off
```

Weather is refreshed about every 30 minutes and the last reading is cached for
offline starts. The launcher sends only the configured coordinates and ordinary
weather request fields to Open-Meteo. It has no account or analytics service.

## Step 7: arrange the screen with the remote

Open **Options** in the top-right corner.

- **Accent color:** colour of the focus and dock outline.
- **Tile size:** small, medium or large.
- **Inputs row:** show or hide hardware inputs.
- **Open inputs with:** automatic is safest; it uses the TV's input app when one is
  unambiguous and the built-in input view otherwise.
- **Background:** solid, built-in wallpaper or adaptive colour.
- **Wallpaper:** Aurora Bay, Alpine Aurora, Golden Coast, or your photos.
- **Wallpaper motion:** still or slow live pan.
- **Image dim:** darkens a bright photo behind text.
- **Apps/Inputs on home screen:** choose what appears without uninstalling it.

On a tile:

| Remote action | Result |
|---|---|
| OK | Open app or input |
| Hold OK, or Menu | Move, rename, hide or open app info |
| Left/Right in Move mode | Reorder the tile |
| Back on Home | Jump to the first tile |
| Home from an app | Return to the launcher |

## Optional: remote shortcut buttons

Mappings are off by default. On the exact TCL 50T6C-UK / compatible remote used
for the screenshots:

```sh
./launcher/configure.sh --profile tcl-50t6c-uk
```

This maps Netflix→Plex, Prime Video→AirPlay help, and Freely→Spotify. Check that
Plex and Spotify are installed first. Other remotes can use different scan codes,
even when the button labels match, so use an AI agent to inspect and configure the
specific remote instead of copying this profile.

## Optional: full Fire OS cleanup

Read the proposed changes first:

```sh
./scripts/preflight.sh
./scripts/debloat.sh --dry-run
```

Then answer the interactive questions and run the guarded sequence:

```sh
./scripts/configure.sh
./scripts/all.sh
./scripts/verify.sh
```

The defaults keep Prime Video, Alexa, every user app and YouTube. Nothing is
removed unless you explicitly name it. DNS filtering is optional. A successful
finish says `all checks passed`.

## The real result on a TCL 50T6C-UK

These are direct 1920×1080 captures from the television, not design mock-ups.

### Home

![Aurora Bay home on the TCL 50T6C-UK](assets/screens/real-tv/tcl-50t6c-home.png)

The top-left block contains time, date, local weather and a greeting that changes
through the day. Apps and inputs sit at the bottom so wallpaper remains visible.

### Options

![Options on the TCL 50T6C-UK](assets/screens/real-tv/tcl-50t6c-options.png)

All visual changes are remote-friendly and saved immediately.

### Another included wallpaper

![Alpine Aurora on the TCL 50T6C-UK](assets/screens/real-tv/tcl-50t6c-alpine.png)

The built-in wallpapers can be still or move with a very slow pan. System reduced
motion/disabled animation settings are respected.

## Verify before declaring success

Test these in order:

1. Press Home from two different apps.
2. Reboot and press Home again.
3. Open an app, the official app store and Live TV.
4. Open every HDMI input, then return with Back/Home.
5. Test casting/AirPlay if it existed before.
6. Scroll beyond the first six apps and confirm no partial tile remains at an edge.
7. Confirm the city and temperature unit.
8. Test only the shortcut buttons you intentionally configured.
9. On a full Fire OS setup, run `./scripts/verify.sh`.

If a core feature fails, undo before experimenting with system packages.

## Undo and recover

Give Home back to the stock launcher:

```sh
./launcher/install.sh --undo
```

On Android/Google TV, choose the original launcher if Android shows a picker. Once
the original Home works, the custom app can be removed:

```sh
adb uninstall tv.dumbtv.home
```

Undo the full Fire OS cleanup:

```sh
./scripts/restore.sh -y
```

If the TV is usable but ADB will not reconnect, toggle ADB debugging off and on in
Developer Options. Never factory-reset merely to fix this launcher.

## Troubleshooting

| Symptom | What to do |
|---|---|
| `adb devices` says `unauthorized` | Approve the prompt on the TV |
| More than one device is listed | Set `ADB_SERIAL` to the intended TV; never guess |
| Stock Home returns on Fire OS | Run `./launcher/install.sh` again; an update may have reset the hook |
| Android/Google TV ignores the new Home | Select it in the Home chooser; if the vendor blocks it, use it as a normal app |
| Weather says unavailable | Check TV internet access and the coordinates; cached data remains visible when available |
| App is missing from the dock | Options → Apps on home screen |
| Input is missing | Confirm the TV exposes it and Options → Inputs on home screen |
| An optional mapping opens nothing | Install/check the destination app or remove the mapping with `none` |

More model-specific fixes are in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
