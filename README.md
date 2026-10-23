# Apple TV-style dumbtv home

An open-source, Apple TV-inspired home screen for Fire TV, Android TV and Google
TV, plus the proven privacy cleanup from
[humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv).

It keeps the useful parts of a television—apps, Live TV, HDMI inputs, casting and
the app store—while replacing the noisy home screen with a fast, quiet dock. It
does not root the TV, flash firmware or replace the operating system.

> [!IMPORTANT]
> Full cleanup automation is for **Fire OS 6, 7 and 8**. The launcher can also be
> installed on many **Android TV and Google TV** devices with ADB, but some brands
> do not allow a third-party app to become the permanent Home screen. Samsung
> Tizen, LG webOS, Roku and VIDAA do not run Android APKs. See
> [compatibility](#compatibility) before starting.

![The finished launcher running on a TCL 50T6C-UK Fire TV](docs/assets/screens/real-tv/tcl-50t6c-home.png)

_Real 1920×1080 screenshot from a TCL 50T6C-UK Fire TV. No mock-up and no personal
network identifiers._

## The simplest setup: give this prompt to an AI coding agent

Use an agent that can run terminal commands on your computer, such as Codex,
Claude Code, Gemini CLI or Cursor. Open the cloned repository in the agent, then
paste this:

```text
Set up my own TV with this repository. Read AGENTS.md and docs/AI-SETUP.md in full
before running anything. My TV model is [MODEL], its operating system is [Fire OS,
Android TV or Google TV], its local IP address is [IP], and my nearest city is
[CITY, COUNTRY]. I want the Apple TV-style home screen shown in the README. Keep
Live TV, every HDMI input, casting/AirPlay, the app store and all my installed apps.
Do a read-only compatibility check first, explain exactly what will change, and ask
before the first change. Do not root, flash, factory-reset or write to /system. If
my TV is not safely supported, stop and tell me. When finished, verify the result
and give me the exact restore command.
```

That is enough for most people. The agent will ask whether you use Prime Video,
Alexa, the DNS filter, YouTube and any branded remote buttons. It must not guess.
The detailed agent workflow is in [docs/AI-SETUP.md](docs/AI-SETUP.md).

## What it will do to your TV

The appearance-only install:

- Adds this lightweight home-screen app. It displays the apps already installed
  on your TV; it does not provide paid apps or bypass subscriptions.
- Shows HDMI and other hardware inputs that the TV exposes through Android's TV
  input system. Live TV remains installed.
- Adds a clock, date, time-aware greeting and optional weather from Open-Meteo.
- Includes three wallpapers, optional slow live motion and support for your own
  photos.
- Keeps an Appstore/Play Store tile when the TV exposes one.
- Can optionally remap branded remote buttons. Remapping is **off by default** and
  is configured for the specific remote only after the owner asks.

The optional Fire OS cleanup also:

- Disables ad, sponsored-content, telemetry, ACR and experiment packages. Packages
  are disabled for the current user, not deleted from the system.
- Turns off ad tracking, usage reporting and promotional autoplay.
- Can run an on-TV DNS filter to block Amazon telemetry and update hosts.
- Can switch off Alexa, remove an app or replace YouTube only when the owner says
  so by name.

Every repository change is reversible with `./scripts/restore.sh -y`. The launcher
alone is undone with `./launcher/install.sh --undo`.

## What the finished interface looks like

| Home screen | Wallpaper choices |
|---|---|
| ![Aurora Bay home screen](docs/assets/screens/real-tv/tcl-50t6c-home.png) | ![Wallpaper options on the TV](docs/assets/screens/real-tv/tcl-50t6c-options.png) |

![Alpine Aurora live-wallpaper option](docs/assets/screens/real-tv/tcl-50t6c-alpine.png)

The dock scrolls one complete tile at a time, shows the selected app's name, and
keeps the inputs in a separate aligned row. The greeting changes between morning,
afternoon, evening and night.

## Manual quick start

### 1. Enable debugging

**Fire TV:** Settings → My Fire TV (or Device & Software) → About → press OK on
the device name seven times → Developer Options → ADB debugging on. Find the IP
under About → Network. On some Fire TV Edition televisions it is under Settings →
Network → the connected Wi-Fi network.

**Android/Google TV:** Settings → System → About → press Build seven times →
Developer options → USB/network debugging or Wireless debugging. Android 11 and
later may show a pairing address and a separate connection address.

Keep the TV and computer on the same private home network. Accept the **Allow USB
debugging?** message on the TV and tick **Always allow**.

### 2. Get the project and connect

Install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools)
so the `adb` command is available, then:

```sh
git clone https://github.com/mahirm380/TV-Debloater.git
cd TV-Debloater
adb connect <TV-IP>:5555
adb devices
```

The TV must appear with the word `device`, not `unauthorized` or `offline`. If the
TV displays a six-digit Wireless debugging code, use `adb pair <PAIR-IP>:<PORT>`
first and enter the code shown on the TV.

### 3A. Install only the Apple TV-style home screen

```sh
./launcher/install.sh
./launcher/configure.sh \
  --weather-name "London" --latitude 51.5074 --longitude -0.1278 \
  --timezone Europe/London --unit celsius
```

The repository includes a checksum-verified APK, so this route does not need
Android Studio, Gradle or a Java setup. On standard Android/Google TV, select
**dumbtv home → Always** if Android shows a Home app chooser.

To use the exact remote-button layout from the TCL 50T6C-UK example—Netflix to
Plex, Prime Video to the built-in AirPlay help screen, and Freely to Spotify—run:

```sh
./launcher/configure.sh --profile tcl-50t6c-uk
```

Do not use that profile on another remote unless its scan codes have been checked.
Package-based interception may still work, but remote hardware codes differ.

### 3B. Fire OS: launcher plus the privacy cleanup

```sh
./scripts/preflight.sh
./scripts/configure.sh
./scripts/all.sh
./scripts/verify.sh
```

`preflight.sh` changes nothing. `configure.sh` asks what you use. `all.sh` confirms
each stage and normally reboots the TV twice. `verify.sh` must finish with
`all checks passed`.

For screenshots of every result, troubleshooting and the exact recovery flow, read
the [beginner installation guide](docs/INSTALL-GUIDE.md).

## Compatibility

| Television platform | Apple-style launcher | Automated cleanup | Important limit |
|---|---:|---:|---|
| Fire OS 6–8 | Yes, tested | Yes | Fire OS 5 and older are refused |
| Android TV / Google TV, Android 7.1+ | Usually | No; use the separate reviewed device lists | Some vendors block replacement Home apps |
| Samsung Tizen | No | No | Does not run Android APKs |
| LG webOS | No | No | Developer mode installs webOS apps only |
| Roku TV | No | No | Developer mode does not provide a shell |
| Hisense VIDAA / Philips Saphi | No | No | Not Android; no owner ADB shell |

“It has developer mode” does not always mean “it can run this launcher.” The
full platform explanation and useful alternatives are in
[docs/OTHER-TVS.md](docs/OTHER-TVS.md).

## Personalising the launcher

Everything visible can be changed with the TV remote under **Options**:

- accent colour and tile size;
- built-in or personal wallpaper;
- still or slow live motion;
- image dimming and wallpaper rotation;
- which apps and inputs appear;
- app/input order and names (hold OK or press Menu on a tile).

Weather and optional remote shortcuts are configured from the computer:

```sh
./launcher/configure.sh --help
```

The settings file is
`/sdcard/Android/data/tv.dumbtv.home/files/home.conf`. Unknown keys are kept, so
updating the launcher does not erase personal choices. See
[launcher/README.md](launcher/README.md) for every setting.

## Safety and privacy

- No root, bootloader unlock, firmware flashing, factory reset or `/system` write.
- No account passwords are requested or stored.
- No IP address, Wi-Fi name, hardware serial or account email belongs in an issue
  or commit. The screenshots here contain none.
- The launcher has no ads, analytics, account system or update checker. Its only
  network request is the optional current-weather request to Open-Meteo.
- AirPlay works only when the television already provides an AirPlay receiver.
  This project does not emulate, bypass or add Apple's protected receiver service.
- Use it only on a television you own or are authorised to manage.

Read [SECURITY.md](SECURITY.md), [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md) and
[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) before changing an unknown
model manually.

## Undo

Launcher only:

```sh
./launcher/install.sh --undo
adb uninstall tv.dumbtv.home     # optional: remove the app too
```

Full Fire OS cleanup:

```sh
./scripts/restore.sh -y
```

The restore script re-enables packages, removes network rules and gives HOME back
to the stock launcher. Apps the owner explicitly asked to uninstall must be
reinstalled from the device's official app store.

## Tested example

The interface shown above was installed and verified on a **TCL 50T6C-UK Fire
TV**, Fire OS 8 / Android 11, over Wi-Fi ADB. Live TV, HDMI inputs, the Amazon
Appstore, AirPlay, Plex and Spotify were retained. The device report is at
[catalog/devices/fireos-tcl-50t6c-uk.yaml](catalog/devices/fireos-tcl-50t6c-uk.yaml).

Other verified devices are listed under [catalog/devices](catalog/devices/). A
successful report from one model is evidence for that build, not a promise about
every television carrying the same brand.

## Build and verify from source

The launcher is plain Java with no Gradle dependencies. It targets Android 7.1 so
one APK covers Fire OS 6–8 and compatible Android TV devices.

```sh
./launcher/build.sh
python3 -m unittest discover -s tests
python3 catalog/validate.py
for file in scripts/*.sh launcher/*.sh; do bash -n "$file"; done
```

Detailed build instructions are in [launcher/README.md](launcher/README.md).

## Credits, licence and trademarks

This is a customised fork of
[humblemuzzu/dumbtv](https://github.com/humblemuzzu/dumbtv). The privacy scripts,
safe package lists, restore flow, device catalog and original launcher architecture
come from that project and its contributors. Android/Google TV research also draws
on [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).

Code and documentation are provided under the [MIT licence](LICENSE); device
catalog reports use [CC BY-SA 4.0](catalog/LICENSE). The bundled wallpapers are
project assets. Apple, Apple TV, AirPlay, Amazon, Fire TV, Alexa, Google TV and
other product names are trademarks of their owners. This independent project is
not affiliated with or endorsed by them.
