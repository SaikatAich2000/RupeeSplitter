# Brand

## Name

**Rupee Splitter**

## Tagline

> Split an amount into ₹1,999 portions.

## Logo

The mark is a gradient squircle that combines two ideas:

1. a **₹** glyph — the currency, and
2. three **decreasing bars** — the portions, a whole breaking into parts.

| Asset | File | Purpose |
| --- | --- | --- |
| In-app vector | `app/src/main/res/drawable/ic_logo.xml` | Crisp at any size, used in the header |
| Launcher (square) | `app/src/main/res/mipmap-*/ic_launcher.png` | Home screen, all densities |
| Launcher (round) | `app/src/main/res/mipmap-*/ic_launcher_round.png` | Round launchers |
| Documentation | `docs/assets/logo.svg` | README and web |

Launcher icons are regenerated with:

```powershell
powershell -ExecutionPolicy Bypass -File tools\generate_launcher_icons.ps1
```

## Colour palette

| Role | Light | Dark | Used for |
| --- | --- | --- | --- |
| Primary | `#3D4CFF` | `#AEB8FF` | Buttons, links, accents |
| Gradient | `#5566FF → #2A2FD8` | `#3A44C9 → #1E2380` | Hero card, logo |
| Background | `#F5F7FB` | `#10131A` | Screen background |
| Surface | `#FFFFFF` | `#191E29` | Cards |
| Text primary | `#121826` | `#F2F4F7` | Headlines, numbers |
| Text secondary | `#667085` | `#B3BBCB` | Supporting text |
| Error | `#BA1A1A` | `#FFB4AB` | Validation |

Every colour exists twice — `res/values/colors.xml` (light) and
`res/values-night/colors.xml` (dark). The Material 3 theme follows the system setting
automatically.

## Typography

- System font (Roboto on Android).
- Bold for numbers and headings.
- Amount input: **24sp bold**. Hero amount: **30sp bold**. Row amounts: **16sp bold**.

## Spacing and shape

- Screen padding: 20dp.
- Card corner radius: 16–24dp. Buttons: 16dp. Chips: 20dp.
- Transitions: 250ms, decelerate — quick and calm.

## Voice and tone

Simple, neutral, precise. The app never promises a tax, legal or fee benefit — it only
does arithmetic, and says so.
