#!/usr/bin/env bash
#
# Change a user's password on the deployed app, then prove it worked.
#
# Run it ON THE SERVER:   bash /opt/cancer/change-password.sh
#
# The password is READ FROM A PROMPT, never passed as an argument and never written to a file.
# An argument would land in shell history and in `ps` output while the script runs; a temp file
# would sit on disk. Both are avoidable, so they are avoided.
#
# Why the verification at the end is not optional: UserService did not hash passwords on update
# until 2026-08-11. A change would return 200 and then never log in again, because
# authentication compares against a BCrypt hash and the column held plaintext. This script
# checks the stored value is a real 60-character BCrypt hash before telling you it worked.

set -uo pipefail

API="${API:-http://localhost:8081}"
USERNAME="${1:-jeb}"

echo "Changing the password for '$USERNAME' on $API"
echo

# --- current password, to authenticate the change ---
read -rsp "Current password for $USERNAME: " CURRENT; echo
if [ -z "$CURRENT" ]; then echo "ERROR: no password entered."; exit 1; fi

# --- new password, twice ---
read -rsp "New password: " NEW1; echo
read -rsp "New password again: " NEW2; echo
if [ "$NEW1" != "$NEW2" ]; then echo "ERROR: the two entries do not match."; exit 1; fi
if [ ${#NEW1} -lt 12 ]; then
  echo "ERROR: use at least 12 characters. This guards one person's medical record on a"
  echo "       public host, and the login endpoint is reachable from the internet."
  exit 1
fi
if [ "$NEW1" = "$CURRENT" ]; then echo "ERROR: new password is the same as the current one."; exit 1; fi
echo

# --- log in ---
# --data-binary @- reads the body from stdin, so the password never appears in the process list.
TOKEN=$(printf '{"username":"%s","password":"%s"}' "$USERNAME" "$CURRENT" \
  | curl -s --max-time 30 -X POST "$API/api/auth/login" \
      -H 'Content-Type: application/json' --data-binary @- \
  | sed 's/.*"token":"//;s/".*//')

if [ ${#TOKEN} -lt 20 ]; then
  echo "ERROR: login failed with the current password."
  echo "       If you have tried several times, the rate limiter locks an account for 15"
  echo "       minutes after 8 failures. 'systemctl restart cancer' clears it."
  exit 1
fi
echo "Logged in."

# --- find the user's extid ---
EXTID=$(curl -s --max-time 30 -H "Authorization: Bearer $TOKEN" "$API/api/user?size=50" \
  | tr '{' '\n' | grep "\"username\":\"$USERNAME\"" | sed 's/.*"extid":"//;s/".*//' | head -1)

if [ -z "$EXTID" ]; then echo "ERROR: could not find a user named '$USERNAME'."; exit 1; fi
echo "Found $USERNAME ($EXTID)."

# --- change it ---
printf '{"password":"%s"}' "$NEW1" \
  | curl -s --max-time 30 -X PUT "$API/api/user/$EXTID" \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      --data-binary @- > /dev/null
echo "Update sent."
echo

# --- verify, three ways ---
echo "Verifying:"

# 1. The stored value must be a real BCrypt hash, not plaintext. This is the check that would
#    have caught the unhashed-password bug immediately instead of an hour later.
STORED=$(mysql -u root -N -B -e \
  "SELECT CONCAT(LENGTH(password),':',LEFT(password,4)) FROM cancer.user WHERE username='$USERNAME';" 2>/dev/null)
LEN="${STORED%%:*}"; PFX="${STORED##*:}"
if [ "$LEN" = "60" ] && [ "${PFX:0:3}" = "\$2a" -o "${PFX:0:3}" = "\$2b" ]; then
  echo "  [ok]   stored value is a 60-character BCrypt hash"
else
  echo "  [FAIL] stored value is $LEN chars starting '$PFX' - NOT a BCrypt hash."
  echo "         The password was stored in plaintext and login will fail."
  echo "         Do not log out of this session. Report this before going further."
  exit 1
fi

# 2. The new password must work.
NEWCODE=$(printf '{"username":"%s","password":"%s"}' "$USERNAME" "$NEW1" \
  | curl -s -o /dev/null -w '%{http_code}' --max-time 30 -X POST "$API/api/auth/login" \
      -H 'Content-Type: application/json' --data-binary @-)
[ "$NEWCODE" = "200" ] && echo "  [ok]   new password logs in" \
                       || { echo "  [FAIL] new password returned $NEWCODE"; exit 1; }

# 3. The old one must not.
OLDCODE=$(printf '{"username":"%s","password":"%s"}' "$USERNAME" "$CURRENT" \
  | curl -s -o /dev/null -w '%{http_code}' --max-time 30 -X POST "$API/api/auth/login" \
      -H 'Content-Type: application/json' --data-binary @-)
[ "$OLDCODE" = "401" ] && echo "  [ok]   old password rejected" \
                       || echo "  [warn] old password returned $OLDCODE (expected 401)"

echo
echo "Done. Store the new password in a password manager - there is no reset flow."
