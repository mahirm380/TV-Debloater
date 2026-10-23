# Set up a TV with an AI coding agent

This guide is written so a terminal-capable AI agent can install the launcher and,
on supported Fire OS devices, perform the optional privacy cleanup without
improvising on a household television.

A normal chat bot can explain these steps, but it cannot operate the computer or
TV. Use a coding agent with terminal access, such as Codex, Claude Code, Gemini
CLI, Cursor, or an equivalent tool. The user does not need to download this
repository first; the agent should do that work.

## Copy this prompt

The owner replaces only the TV IP address.

```text
I own the TV at [TV IP ADDRESS]. ADB/Wireless debugging is already enabled and the
TV is on the same private network as this computer. Set it up using
https://github.com/mahirm380/TV-Debloater.

Do all of the computer work for me: download the repository, obtain Android ADB
from the official source if it is missing, connect to the TV, detect its model and
operating system, and read AGENTS.md and docs/AI-SETUP.md before changing anything.
Do not ask me to clone anything, install tools, or type terminal commands myself.

I want the Apple TV-style home screen shown in the repository. Keep Live TV, every
HDMI input, casting/AirPlay, the official app store, and all installed user apps.
Use safe defaults and leave remote-button remapping off unless I request it. Run a
read-only compatibility check first, show me a short summary, and ask once before
the first change. I will use the TV remote only to accept its debugging prompt or
choose dumbtv home if the TV asks.

Never root, flash, factory-reset, unlock the bootloader, write to /system, or guess
model-specific package removals. If this TV is not safely supported, stop and
explain why. When finished, configure the launcher, verify Home, apps, Live TV,
HDMI inputs and the app store, then give me the restore command.
```

The user should not paste an account password, Wi-Fi password, serial number or MAC
address into the prompt. A local IP address is needed for the session but must not
be committed, posted in an issue or included in a screenshot.

## What the agent must do

### 0. Prepare the computer without delegating work to the owner

If the repository is not already open, clone
`https://github.com/mahirm380/TV-Debloater` into a normal work folder and work from
that copy. If Git is unavailable, download the repository's source archive instead.

If `adb` is missing, download Android SDK Platform-Tools from Google's official
developer site into a user-writable tool folder, verify the supplied checksum when
one is published, and use its `adb` binary directly. Do not ask the owner to install
Homebrew, Android Studio, a system package or a global dependency merely for this
setup.

Do not ask the owner to run commands. The owner only needs to approve prompts on
the television, provide a pairing code if Android displays one, answer preferences
that cannot be detected safely, and approve the proposed changes.

### 1. Read the safety rules

Read [../AGENTS.md](../AGENTS.md) completely before the first command. Its hard
rules override convenience. In particular:

- never root, remount, write to `/system`, flash or factory-reset;
- never disable a package on a `never-disable.txt` list;
- never remove an app the owner did not name;
- never guess package names or model-specific remote codes;
- operate on one TV only;
- ask before the first change;
- keep all device and household identifiers out of the repository.

### 2. Confirm the platform without changing it

Connect, wait for the owner to approve the prompt on the TV, then run:

```sh
adb connect <TV-IP-OR-CONNECTION-ADDRESS>
adb devices
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.display.id
adb shell pm list packages
```

If several devices are connected, set `ADB_SERIAL` to the exact TV entry before
doing anything else.

Classify the result:

| Finding | Safe route |
|---|---|
| `com.amazon.tv.launcher` exists; Android SDK 25, 28 or 30 | Fire OS 6, 7 or 8: full workflow is available |
| Android/Google TV; Android SDK 25 or newer | launcher-only workflow; reviewed cleanup remains model-specific |
| Fire OS 5 or Android SDK below 25 | stop; launcher and disable commands are unsupported |
| Samsung Tizen, LG webOS, Roku, VIDAA, Saphi | stop; these platforms cannot run this Android launcher |
| Manufacturer/model cannot be identified | stop and report the read-only results |

Do not treat an open network port as proof of compatibility. Check the operating
system and installed packages.

### 3. Explain the proposed changes

Before asking for approval, tell the owner which route applies.

For a launcher-only install:

- one APK, package `tv.dumbtv.home`, will be installed;
- Android will be asked to use it for Home;
- no existing launcher or app will be disabled;
- optional weather makes requests only to Open-Meteo;
- remote remaps remain off unless explicitly requested.

For the Fire OS cleanup, also summarise the output of:

```sh
./scripts/preflight.sh
./scripts/debloat.sh --dry-run
```

Then ask the questions in [../AGENTS.md](../AGENTS.md): Prime Video, Alexa,
launcher, DNS filter, explicitly named removals and optional YouTube replacement.
Wait for the owner's approval before the first change.

### 4A. Launcher-only workflow

Install the checksum-verified bundled APK:

```sh
./launcher/install.sh
```

On standard Android/Google TV, the owner may need to select **dumbtv home** and
**Always** in the Home app chooser. If the vendor refuses a third-party default
launcher, leave the stock launcher enabled and explain that dumbtv home can still
be opened from Apps. Do not disable an unknown stock launcher as a workaround.

Configure the weather location supplied by the owner. The agent should resolve the
city to ordinary latitude/longitude coordinates and use the correct IANA timezone:

```sh
./launcher/configure.sh \
  --weather-name "<CITY>" \
  --latitude <LATITUDE> \
  --longitude <LONGITUDE> \
  --timezone <AREA/CITY> \
  --unit <celsius-or-fahrenheit>
```

Use `--weather-off` when the owner does not want the network weather request.

### 4B. Full Fire OS workflow

Record the owner's answers, run each guarded stage and verify:

```sh
./scripts/configure.sh
./scripts/all.sh
./scripts/verify.sh
```

If the TV drops Wi-Fi ADB during a reboot, wait for it to finish booting and ask
the owner to toggle ADB debugging off and on. Do not reconnect in a tight loop and
do not change the TV's network configuration.

### 5. Optional remote buttons

Button changes are never part of the default install. Ask the owner to name each
source button and destination app. Check that every destination package exists on
the TV before configuring it.

The tested TCL 50T6C-UK profile is:

```sh
./launcher/configure.sh --profile tcl-50t6c-uk
```

It maps Netflix to Plex, Prime Video to the launcher's AirPlay help screen and
Freely to Spotify. Its hardware scan codes are 744, 745 and 746. Apply it only to
that TV/remote family or after independently observing the same codes.

For other Fire TV remotes, package-based interception can be configured without a
scan code:

```sh
./launcher/configure.sh --netflix com.plexapp.android --prime airplay
```

If a button never launches the branded app and no verified scan code is available,
leave it unchanged. Do not copy random codes from the internet.

### 6. Verify what the owner cares about

After installation, check on the television—not only through command output:

1. Home opens the new launcher repeatedly and after a reboot.
2. The remote can move across the complete app tiles without clipped edges.
3. The selected app name appears and the app opens.
4. Live TV/tuner still opens.
5. Every connected HDMI input opens and returns with Back or Home.
6. Casting/AirPlay still appears to a phone when the TV already supported it.
7. Appstore/Play Store opens.
8. Weather shows the requested city or is hidden as requested.
9. Optional branded-button mappings work and unconfigured buttons are untouched.
10. For the full Fire OS route, `./scripts/verify.sh` exits 0.

Capture screenshots with:

```sh
adb exec-out screencap -p > tv-home.png
```

Inspect screenshots before sharing. Delete or crop anything containing an IP
address, Wi-Fi name, account email, hardware serial or private notification.

### 7. End with recovery instructions

Always give the owner both commands:

```sh
./launcher/install.sh --undo
./scripts/restore.sh -y       # only when the Fire OS cleanup was run
```

On Android/Google TV, `--undo` clears the Home preference and may show Android's
Home app picker. The owner should choose the original launcher. `adb uninstall
tv.dumbtv.home` removes the custom launcher after Home has been restored.

## Handling common agent mistakes

- **The prompt says “all TVs.”** Interpret that as every supported Android-based
  TV that exposes the needed owner ADB functions, not every brand with a developer
  menu. Report platform limits honestly.
- **The user asks for AirPlay.** Preserve an existing receiver. Do not claim the
  launcher adds Apple's receiver service; its AirPlay tile is a help screen.
- **The user asks for an app store.** Expose the official store already present.
  Do not download paid APKs or unofficial stores without a separate explicit ask.
- **A command says permission denied.** Stop and explain it. Never escalate to
  root, remount `/system`, disable security or factory-reset.
- **A feature fails on one model.** Restore the last reversible change, keep core
  TV functions working, and report the model/build. Do not broaden a package list
  based on its name alone.

## Example: the TV shown in this repository

The reference installation used:

```text
Model: TCL 50T6C-UK
Platform: Fire OS 8 / Android 11
Connection: Wi-Fi ADB
Weather: London, Europe/London, Celsius
Keep: Live TV, all HDMI inputs, AirPlay, Amazon Appstore, Netflix
Apps: Plex and Spotify
Buttons: Netflix→Plex, Prime Video→AirPlay, Freely→Spotify
```

The result and Options screen are shown in
[the installation guide](INSTALL-GUIDE.md#the-real-result-on-a-tcl-50t6c-uk).
