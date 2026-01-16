# LockInApp

LockInApp is an Android productivity app designed to help you **restrict access to distracting apps and websites** during focus periods.
The goal is simple: when you decide to focus, your phone should respect that decision.

This project is currently **under active development**.

---

## What LockInApp Does Today

### App Blocking (Working)

* Block selected apps (e.g. Instagram, YouTube, Facebook)
* Multiple blocking modes:

  * **Indefinite** (blocked until password is entered)
  * **Duration-based** (blocked for X minutes)
  * **Time interval** (e.g. 22:00–07:00)
* Password-protected unlock
* Uses Android **Accessibility Service** to reliably block apps in the foreground

### App Selection

* Lists all launchable apps installed on the device
* Search by app name or package name
* Prioritizes commonly distracting apps for convenience

---

## VPN / Website Blocking (Work in Progress)

LockInApp is currently being extended with a **VPN-based blocking system** to support:

* Blocking websites across **all browsers**
* Blocking apps that load content via webviews
* Blocking even when **Private DNS / Secure DNS** is enabled

### Current VPN Status

* A **full-tunnel VPN architecture** is being implemented (not pushed yet)
* Uses a **tun2socks-based engine** to correctly forward TCP traffic
* DNS interception and domain blocking are already in place
* TCP forwarding is actively being integrated and stabilized

At this stage:

* App blocking is stable and usable
* Website blocking via VPN is **not yet production-ready** (it has a small bug and blocks all sites in loading status)
* Expect partial connectivity issues while the VPN engine is under development

This is intentional and part of the development roadmap.

---

## Why a VPN Is Needed

Android does not allow normal apps to:

* Block websites system-wide
* Inspect traffic from other apps
* Intercept encrypted DNS reliably

A local VPN is the **only correct and supported approach** to achieve system-wide website blocking without rooting the device.

---

## Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Persistence:** SharedPreferences (Room planned)
* **App Blocking:** AccessibilityService
* **Website Blocking:** VpnService + tun2socks engine (in progress)

---

## Current Limitations

* VPN mode may temporarily break connectivity
* Some browsers using DNS-over-HTTPS may bypass DNS blocking (being addressed)
* Not intended for Google Play release! So I am only developing it for personal use.

---

## Roadmap

* App blocking with password protection
* Duration and interval rules
* Stable full-tunnel VPN (TCP forwarding)
* Website blocking with DNS + SNI filtering
* UI polish and settings refinement
* Optional statistics and focus reports

---

## Status

**Active development.**
The project is experimental, technical, and focused on correctness rather than quick hacks.

If you’re reading this and something feels unfinished — it probably is, by design.

---

If you want, I can also:

* write a **short Play Store–style description** (safe wording)
* add a **technical architecture section**
* or split this into `README.md` + `DEV_NOTES.md` for clarity

![WhatsApp Image 2026-01-14 at 16 38 55](https://github.com/user-attachments/assets/c8eed774-3227-46e3-b10b-1eaff62610b0)
