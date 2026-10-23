#!/usr/bin/env bash
# Personalise dumbtv home after it is installed. Existing on-TV choices are preserved.
#
# Examples:
#   ./launcher/configure.sh --weather-name London --latitude 51.5074 --longitude -0.1278
#   ./launcher/configure.sh --netflix plex --prime airplay --freely spotify
#   ./launcher/configure.sh --profile tcl-50t6c-uk
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

ADB=${ADB:-adb}
PKG=tv.dumbtv.home
CONFIGURE=$PKG/$PKG.Configure

adb_() { "$ADB" ${ADB_SERIAL:+-s "$ADB_SERIAL"} "$@"; }
sh_() { adb_ shell "$@" </dev/null; }
fail() { printf 'error: %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'EOF'
usage: launcher/configure.sh [choices]

Weather:
  --weather-name NAME       label shown beside the temperature
  --latitude NUMBER         -90 to 90
  --longitude NUMBER        -180 to 180
  --timezone ZONE           IANA zone such as Europe/London (default: auto)
  --unit celsius|fahrenheit
  --weather-on              show weather
  --weather-off             hide weather completely

Optional branded remote buttons (disabled unless set):
  --netflix TARGET          package name, plex, airplay, or none
  --prime TARGET            package name, plex, airplay, or none
  --freely TARGET           package name, spotify, airplay, or none
  --netflix-scan NUMBER     optional hardware scan code
  --prime-scan NUMBER       optional hardware scan code
  --freely-scan NUMBER      optional hardware scan code

Ready-made profile:
  --profile tcl-50t6c-uk   Netflix→Plex, Prime→AirPlay, Freely→Spotify

Connection:
  Set ADB_SERIAL when more than one device is connected.
EOF
}

valid_number() { [[ $1 =~ ^-?[0-9]+([.][0-9]+)?$ ]]; }
in_range() { awk -v number="$1" -v low="$2" -v high="$3" 'BEGIN { exit !(number >= low && number <= high) }'; }
valid_name() { [[ $1 != *$'\n'* && $1 != *'='* && -n $1 ]]; }
valid_timezone() { [[ $1 =~ ^[A-Za-z0-9_+./:-]+$ ]]; }
valid_target() { [[ $1 == none || $1 == plex || $1 == spotify || $1 == airplay || $1 =~ ^[A-Za-z0-9_]+([.][A-Za-z0-9_]+)+$ ]]; }
valid_scan() { [[ $1 =~ ^[0-9]+$ ]]; }

declare -a changes=()
set_value() { changes+=("$1=$2"); }

while [[ $# -gt 0 ]]; do
  case $1 in
    --weather-name) [[ $# -ge 2 ]] || fail "$1 needs a value"; valid_name "$2" || fail "invalid weather name"; set_value weather on; set_value weather.name "$2"; shift 2 ;;
    --latitude) [[ $# -ge 2 ]] || fail "$1 needs a value"; valid_number "$2" && in_range "$2" -90 90 || fail "latitude must be from -90 to 90"; set_value weather on; set_value weather.latitude "$2"; shift 2 ;;
    --longitude) [[ $# -ge 2 ]] || fail "$1 needs a value"; valid_number "$2" && in_range "$2" -180 180 || fail "longitude must be from -180 to 180"; set_value weather on; set_value weather.longitude "$2"; shift 2 ;;
    --timezone) [[ $# -ge 2 ]] || fail "$1 needs a value"; valid_timezone "$2" || fail "invalid timezone"; set_value weather on; set_value weather.timezone "$2"; shift 2 ;;
    --unit) [[ $# -ge 2 ]] || fail "$1 needs a value"; [[ $2 == celsius || $2 == fahrenheit ]] || fail "unit must be celsius or fahrenheit"; set_value weather on; set_value weather.unit "$2"; shift 2 ;;
    --weather-on) set_value weather on; shift ;;
    --weather-off) set_value weather off; shift ;;
    --netflix|--prime|--freely)
      [[ $# -ge 2 ]] || fail "$1 needs a value"
      valid_target "$2" || fail "invalid shortcut target: $2"
      set_value "shortcut.${1#--}" "$2"
      shift 2 ;;
    --netflix-scan|--prime-scan|--freely-scan)
      [[ $# -ge 2 ]] || fail "$1 needs a value"
      valid_scan "$2" || fail "scan code must be a positive number"
      name=${1#--}; set_value "shortcut.${name%-scan}.scan" "$2"
      shift 2 ;;
    --profile)
      [[ ${2:-} == tcl-50t6c-uk ]] || fail "the only bundled profile is tcl-50t6c-uk"
      set_value shortcut.netflix plex
      set_value shortcut.netflix.scan 744
      set_value shortcut.prime airplay
      set_value shortcut.prime.scan 745
      set_value shortcut.freely spotify
      set_value shortcut.freely.scan 746
      shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) fail "unknown choice: $1 (run with --help)" ;;
  esac
done

[[ ${#changes[@]} -gt 0 ]] || { usage; exit 2; }
command -v "$ADB" >/dev/null || fail "adb not found — install Android platform-tools"
[[ $(adb_ get-state 2>/dev/null || true) == device ]] || fail "no TV connected"
sh_ pm path "$PKG" >/dev/null 2>&1 || fail "$PKG is not installed yet"

normalise_target() {
  case $1 in
    plex) printf '%s' com.plexapp.android ;;
    spotify) printf '%s' com.spotify.tv.android ;;
    airplay) printf '%s' airplay ;;
    none) printf '' ;;
    *) printf '%s' "$1" ;;
  esac
}

for change in "${changes[@]}"; do
  key=${change%%=*}
  value=${change#*=}
  if [[ $key == shortcut.* && $key != *.scan ]]; then value=$(normalise_target "$value"); fi
  if [[ -n $value ]]; then
    result=$(sh_ am broadcast --receiver-foreground -a tv.dumbtv.home.CONFIGURE \
      -n "$CONFIGURE" --es key "$key" --es value "$value")
  else
    result=$(sh_ am broadcast --receiver-foreground -a tv.dumbtv.home.CONFIGURE \
      -n "$CONFIGURE" --es key "$key" --ez clear true)
  fi
  [[ $result == *"result=1"* ]] || fail "the TV refused setting $key ($result)"
done

sh_ am start -W -n "$PKG/.Home" >/dev/null
printf 'personalised %s; the home screen has been reopened\n' "$PKG"
