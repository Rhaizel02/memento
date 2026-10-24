# Design System: Memento — The Warm Archive

**Status:** approved visual direction · consolidated design contract · v1.0  
**Product:** Memento, native Android application (Jetpack Compose)  
**Interface language:** Spanish (es-ES)  
**Modes:** Warm Light and Warm Dark — **one brand, two appearances**, not multiple themes  
**Use:** paste or attach this file to Google Stitch when generating/refining screens; give the same file to Codex when implementing Compose.

> **One-line art direction:** A private cultural archive that feels like a beautifully typeset journal and a curated collection — warm, editorial, intimate, visually rich, and effortless to use.

## 1. Visual Theme & Atmosphere

### What Memento is

Memento holds cultural memories about **books, films, series and video games**. It has three inseparable layers:

1. **Library:** what I have read, watched, played, am consuming, or want to discover.
2. **Diary:** my progress, notes, quotes, ratings, tags and reflections.
3. **Memory:** the moments and personal cultural history worth revisiting years later.

It is **not** a streaming service, social network, productivity tracker, achievement system or analytics dashboard. External media metadata supports the experience; the user's own memories give it meaning.

### Design personality

- **Atmosphere:** cozy premium editorial; quiet confidence, tactility, warmth and a little nostalgia, without literal paper textures or decorative clutter.
- **Density:** **6/10** overall; **7/10** in Library and quick-action flows; **4/10** in reflective passages, hero moments and Wrapped.
- **Asymmetry:** **6/10**. Use left-aligned headlines, offset artwork, modestly varied section rhythm and editorial whitespace. Keep dense lists orderly.
- **Motion:** **3/10**. Gentle and tactile, not theatrical. No forced animation when reading personal text.
- **Emotional keywords:** private, thoughtful, collected, tactile, literary, cinematic, welcoming, memorable.
- **Not:** generic Material 3, faux-vintage scrapbook, a Netflix clone, a game launcher, a corporate reporting app.

**Visual signature:** warm off-white/espresso backgrounds, deep ink, a single **terracotta action accent**, Newsreader's elegant editorial voice, disciplined Work Sans controls, generous cover art and slim dividers. Let content artwork provide variation instead of using multiple brand accents.

### Source precedence

1. This `DESIGN.md` is authoritative for **tokens, visual behavior, accessibility and supported functionality**.
2. Existing **approved Warm Archive** Stitch screens are references for **composition and emotional style**, not for unsupported features or inconsistent CSS tokens.
3. Actual Android product behavior/data models prevail for the final implementation. Do not create functionality just because a mockup illustrates it.
4. The Stitch export labeled **dark Home** that renders on an ivory background is **not an approved dark-mode reference**. The dark Library and dark Media Detail are stronger tonal references.

---

## 2. Color Palette & Semantic Roles

### Brand-color rule

**One primary accent family only: terracotta.** Use it for primary actions, selected navigation, links and focused controls. Do **not** use olive, burgundy, teal or violet as rival CTA colors. Neutral browns and subdued semantic colors may express status, error or media context, never alternate branding.

The tables below are **the single source of truth**. Do not borrow `#B84D29`, `#C85A32` or a competing Tailwind token from older Stitch HTML unless it is first explicitly adopted here.

### Warm Light — default

| Semantic token | Color | Role |
|---|---|---|
| `background` | `#FFF8F3` | Primary canvas: warm paper, never stark white |
| `surface` | `#FFFDFB` | Content surface and most sheets |
| `surfaceElevated` | `#FFFFFF` | Featured content and input backgrounds, used sparingly |
| `surfaceMuted` | `#F7EFE6` | Subtle contextual blocks, notes and list alternation |
| `surfaceStrong` | `#F2E8DC` | Deliberate section distinction / selected neutral containers |
| `textPrimary` | `#241E1A` | Titles, controls, primary content |
| `textSecondary` | `#6E6259` | Metadata, supportive descriptions |
| `border` | `#D6C8B8` | Structural rules, input borders |
| `borderSoft` | `#E8DED2` | Quiet dividers |
| `accent` | `#9F3C16` | Primary CTA, selection, links, active tab |
| `onAccent` | `#FFFFFF` | Text/icon on accent; contrast > 6:1 |
| `accentSoft` | `#F7E7DD` | Highlight tint and selected states, never full-page background |
| `onAccentSoft` | `#6A260C` | Text on accent-soft surfaces |
| `error` | `#A33A31` | Destructive/error state only |
| `success` | `#48623F` | Successful state only, never a second CTA |

### Warm Dark — equally important, not inverted light

| Semantic token | Color | Role |
|---|---|---|
| `background` | `#181412` | Deep espresso canvas, **not** `#000000` |
| `surface` | `#201B18` | Standard content surface |
| `surfaceElevated` | `#28221E` | Prominent section, sheet and raised content |
| `surfaceMuted` | `#2E2722` | Quiet context/quote surfaces |
| `surfaceStrong` | `#332C27` | Selected neutral blocks |
| `textPrimary` | `#EDE4DC` | Ivory text for long comfortable reading |
| `textSecondary` | `#B5A69B` | Muted metadata, still legible |
| `border` | `#4D4139` | Input outlines, separators |
| `borderSoft` | `#382F29` | Subtle rules |
| `accent` | `#E07752` | Primary CTA and selected states |
| `onAccent` | `#181412` | **Dark** text/icon on light terracotta; never white on this fill |
| `accentSoft` | `#471F14` | Restrained terracotta container |
| `onAccentSoft` | `#F7C9B7` | Text on accent-soft container |
| `error` | `#F49A89` | Destructive/error state only |
| `success` | `#A3B48E` | Success state only |

**Color discipline:** text on `background` is `textPrimary`, not accent. Reserve accent for **meaningful actions**. A recommendation reason, quote or memory should be readable without colored backgrounds. Use border and spacing before adding card fills. A media-type marker may be a tiny, muted semantic cue; do not recolor the whole screen according to content type.

### Contrast and state rules

- Aim at least WCAG AA: **4.5:1** for standard text, **3:1** for large text, icons and focus boundaries.
- Verified reference combinations: light `accent #9F3C16` / white ≈ **6.4:1**; dark `accent #E07752` / espresso `#181412` ≈ **6.0:1**. Dark accent/white is **not** acceptable for standard button text.
- Do not communicate statuses or recommendation levels by color alone; pair with explicit Spanish labels.
- Disabled controls are visually muted but their labels remain intelligible; error and selected states must not look identical.
- Posters and backdrops use soft, content-dependent scrims behind any overlaid text. Never assume poster colors allow readable labels.

---

## 3. Typography & Editorial Hierarchy

**Approved font pairing (do not replace with a generic template):**

- **Newsreader** — brand identity, display headings, editorial section headings, literary quotes, personal reflections and memorable insights. Use 400–500; italic sparingly and meaningfully.
- **Work Sans** — navigation, tabs, metadata, counts, filters, input controls, forms, status labels, button text and all dense functional UI. Use 400–600.
- **JetBrains Mono** — **optional only** for tightly aligned numerics such as dates/progress; not for decorative code aesthetics.

`Inter`, generic Times/Georgia-style visual treatments, condensed athletic fonts, all-caps body text and overly ornamental scripts are not part of the brand. System font fallback is acceptable when a custom font fails to load. Newsreader remains the intentional exception to generic serif bans because this product is an editorial cultural journal, **not** a software dashboard.

| Style token | Font | Size / line height | Weight | Use |
|---|---|---|---|---|
| `displayHero` | Newsreader | **36 / 44 sp** | 400 | Home editorial statement / Wrapped cover |
| `screenTitle` | Newsreader | **32 / 40 sp** | 400 | Biblioteca, Discover, Historia |
| `sectionTitle` | Newsreader | **24 / 32 sp** | 500 | Recuerda, En curso, tus recuerdos |
| `itemTitle` | Newsreader | **20 / 28 sp** | 500 | Featured media / large recommendation |
| `quoteBody` | Newsreader | **18 / 28 sp** | 400 or italic | Quotes and longer private reflections |
| `body` | Work Sans | **16 / 24 sp** | 400 | Main instructional/copy text |
| `bodyCompact` | Work Sans | **14 / 20 sp** | 400 | List descriptions and metadata |
| `label` | Work Sans | **14 / 20 sp** | 600 | Buttons, filters, field labels |
| `caption` | Work Sans | **12 / 16 sp** | 500 | Nonessential overlines/eyebrows only |

- `sp` values are design targets for **Compose**, scalable under Android font-size settings. Use equivalent px in Stitch mockups as visual approximations, **not** fixed web rem as a Compose contract.
- Default body text must not be reduced just to fit more items; 12 sp is reserved for optional captions, not interaction labels or explanatory text.
- Headlines are predominantly **left-aligned**; limited centered layouts for a single work cover or deliberate Wrapped cover.
- Keep prose comfortably short, roughly **55–65 characters per line** at tablet widths; don't force ultra-narrow columns on phones.
- Avoid truncating a title without offering a way to open the full item. Preserve names, accents and typographic punctuation in natural Spanish.
- Do not render entire UI in Newsreader: its expressive quality relies on contrast with utilitarian Work Sans.

---

## 4. Spacing, Shape and Elevation

### Spatial tokens (dp in Android; px in Stitch previews)

- `space1 = 4`, `space2 = 8`, `space3 = 12`, `space4 = 16`, `space6 = 24`, `space8 = 32`, `space10 = 40`, `space12 = 48`.
- Mobile horizontal gutter: **20 dp** typical, **16 dp** on very compact screens.
- Gap between related label/content: **4–8 dp**; between elements in a section: **12–16 dp**; between editorial sections: **24–32 dp**.
- List rows: compact but breathable; artwork column with consistent alignment and metadata hierarchy.
- Bottom navigation stays clear of content and respects Android system insets.

### Shape roles (do not use one radius everywhere)

| Role | Radius |
|---|---|
| Small pills/input subdivisions | **8 dp** |
| Media tile / compact list image | **10 dp** |
| Regular card / input | **12 dp** |
| Highlighted editorial card | **16 dp** |
| Modal bottom-sheet top corners | **24 dp** |
| Filter/status chip | Fully pill-shaped |

**Elevation:** usually 0–2 dp, using warm, low-opacity shadows. Favor fine borders (`borderSoft`) and whitespace over raised rectangular cards. Large shadows and floating glossy tiles are prohibited. Group editorial text using typographic rhythm, not nested cards.

### Artwork

- Covers and posters are the **primary visual material**, with honest original aspect ratios: books generally **2:3**, movie/series posters roughly **2:3**, games use existing provider artwork and a fit suited to the source.
- Maintain consistent crop behavior per component, never stretch or fabricate new cover artwork.
- Artwork is generous in **Detail** and **Discover**; more compact and scannable in **Library**.
- Prefer real provider imagery already available. Use refined neutral placeholders and title/creator when art is missing or offline.
- Backdrops may appear as a quiet upper-page visual with a readable gradient/scrim; never drown controls or personal reflections in imagery.
- Decorative texture, if used, is barely perceptible and **not required** for Compose. Avoid resource-heavy noise layers.

---

## 5. Components and States

### Navigation

- The primary Android navigation has four clear destinations: **Inicio · Biblioteca · Descubrir · Historia** (preserve/adjust existing navigation wiring rather than recreating it unnecessarily).
- **Perfil cultural, Estadísticas, Wrapped and Ajustes** remain secondary destinations via a visible, consistent entry point.
- Active item: terracotta icon/indicator + label; inactive: neutral. Do not use social-profile avatars to suggest accounts or community features.
- Keep bottom navigation compact, readable and reachable; **no five or six competing primary tabs**.

### Buttons and links

- **Primary:** solid `accent`, `onAccent`, 12 dp radius, 48–52 dp min height; one dominant CTA per scene.
- **Secondary:** `surfaceMuted`/outline with `textPrimary` and clear border; no competing accent-colored filled buttons.
- **Tertiary:** text/icon controls with accessible hit areas, used for dismiss, external link, and less common actions.
- Tap feedback: restrained scale ~0.98 / subtle surface change; do not make buttons wobble or glow.
- For dangerous/reversible actions (delete, discard), use semantic messaging and confirmation where needed; never color a benign action like deletion.

### Cards and content groups

- **Remember hero:** a curated memory fragment, small contextual date, clearly featured quote/observation and one reopen action. Use a light editorial inset, not an analytics metric card.
- **In-progress row:** cover, name, concrete progress, and two immediate actions `Actualizar progreso` and `Añadir nota`. Actions must remain visible at normal font scaling.
- **Library row:** small cover, title, creator, status/rating and restrained user tag or category. Prefer rows/dividers to a tower of oversized identical cards.
- **Recommendation card:** artwork, title/creator, tier text, short **evidence-based** explanation. Entire card clickable to Recommendation Detail.
- **Insight block:** editorial headline, specific grounded value, modest secondary explanation. Charts only if needed to clarify patterns.
- **Timeline event:** date grouping, media cue, event type, meaningful excerpt; balanced density and chronological scanability.

### Inputs / quick action sheets

- Fixed labels **above** fields, subtle helper below, errors inline below. State changes use accent borders and accessible focus indications.
- **Update progress:** current amount + unit, one editable new value, appropriate quick increments when meaningful, one Save. Support numerical input affordances and validation. Do not assume all media use pages or percent.
- **Add note / quote:** clear `Nota` / `Cita` choice, one multiline field, one Save; no voice recording, tag selection or auxiliary metadata in this fast path.
- Quote may be saved for an **already completed** work if an existing consumption is available; Note retains its actual active-consumption constraints. Follow existing domain logic, not visual guesswork.
- `Marcar como completada` must lead to the existing finalization flow with historical date and optional rating/favorite/reflection, not silently finish a work.
- Sheets are genuinely compact, keyboard-aware, and dismissible without erasing text accidentally. When keyboard opens, Save remains reachable.

### States are designed, not omitted

Every important screen/component needs: **loading, populated, empty, error, offline/stale, long text, missing artwork, many items, text scaling**. Use precise-size neutral skeletons while loading; no perpetual decorative spinners. Inline retry actions where appropriate. Never fabricate data when empty.

---

## 6. Screen Composition Contracts

These descriptions specify **layout and information hierarchy**, not new features. Stitch may propose composition variations while preserving supported data and navigation.

### A. Home / Inicio — emotional entry, practical actions

- Compact brand identity and personal archive mood; restrained date/eyebrow, then one editorial statement. Avoid a fake volume number or archive serial ID unless real data exists.
- **Recuerda** is the emotional hero, based only on a real saved work/quote/reflection/date. One simple action to reopen its origin.
- **En curso** appears high enough for quick access: artwork, status/progress, `Actualizar progreso` and `Añadir nota`.
- A compact **Tal día como hoy** moment, a **personal recommendation** with grounded reason and a recent activity glimpse; do not put equally large banners on all four.
- Keep useful daily actions within easy reach; the first fold should not be 80% decorative hero.
- In **Warm Dark**, use deep espresso surfaces and ivory text. A screen labeled dark that still uses cream canvas is invalid.

### B. Biblioteca — efficient, beautiful density

- Prominent Newsreader title, Work Sans search field, simple media/status/tag filters, optional grid/list control if functionality already exists.
- Rows or compact tiles show real artwork, title, creator, progress/status and rating where applicable.
- A small, discrete **Añadir nueva obra** action; discoverability without a permanent oversized CTA hiding items.
- Distinguish status with words and small cues, not a separate background color for every item.
- Search must not imply it indexes personal-note text unless that capability exists.

### C. Media Detail / Detalle de obra — the heart of Memento

- Well-composed cover/backdrop, title, author/director/developer, date/genre and status/rating. Clear quick action appropriate to current status.
- Separate **metadata** from **Mis recuerdos / Mi diario** through typography and spacing, not a clutter of nested cards.
- Show existing notes, saved quotes, final/later reflections, personal tags, and cultural events when available. Artwork remains important, but **personal writings must be readable and easy to reach**.
- Maintain action for historical quote capture on completed works. Empty subsections should not dominate the page.
- **Do not invent** photo diary, purchase location, weather, voice memo or book acquisition provenance.

### D. Discover / Descubrir — curated finding, not clickbait

- A compact media-type filter, then varied but coherent candidate cards with artwork, evidence and available metadata.
- Use ONLY **`Muy afín`**, **`Buena apuesta`**, **`Exploración`** as visible tiers; **never** show false `98%`/`99%` probability language or personality guesses.
- Only recommend works not already present in the library; do not inject planned/completed titles as new recommendations.
- The entire card opens Recommendation Detail; tapping a Home recommendation opens **that exact work**, not the Discover landing screen.

### E. Recommendation Detail — attractive and actionable

- Immersive artwork, title/creator/year/genres, external rating **with source if available**, full synopsis, clear recommendation tier and **real evidence** (favorite source works, shared creator/genres, anchor consensus).
- Visual action hierarchy: **Añadir a pendientes** primary; `Empezar ahora`, `Ya la terminé` and `Ver fuera de Memento` secondary; `No me interesa` quiet tertiary.
- Completing must open the actual historical completion flow; do not silently set current date. External links go to valid official provider pages/browser, not an invented Amazon integration.
- No psychological profiling, NLP of reflections, fake compatibility percentage, streaming availability or purchase UI that the product cannot supply.

### F. Historia / Timeline — navigating a life in stories

- Date headings and quiet vertical event rhythm, potentially an editorial timeline rail; events grouped chronologically.
- Support existing event types: **empezada, terminada, progreso, nota, cita, reflexión final, reflexión posterior**.
- Personal text appears as legible excerpts with access to its originating work; long texts expand on tap.
- No invented weather, place, diary voice recording or mood detection.

### G. Perfil cultural / Estadísticas — human insights

- Lead with one meaningful personal insight, then a few well-ordered supporting statistics.
- Only derived evidence: **genres, creators, media types, ratings, favorites, personal tags, completion dates, cultural periods**.
- Respect sample-size thresholds; do not claim a top genre/creator from a single rated work.
- Explain patterns in natural, accurate Spanish. Avoid corporate pie-chart dashboards and unsupported interpretation of journal prose.

### H. Wrapped — private annual keepsake

- Editorial, story-like pages: annual recap, featured work, meaningful genre, standout period and saved memory when data allows.
- Use actual chosen-year data. For ongoing years, explicitly indicate `hasta ahora`; do not imply future months happened.
- No streaks, badges, achievements, music overlays or share-by-default of private reflections.

### I. Settings, onboarding and supporting flows

- Apply the same fonts, tokens, buttons, bottom sheets, system bars and navigation behaviors to secondary screens.
- Onboarding may introduce Library/Diary/Memory with tasteful abstract or locally available artwork; do not require network or unlicensed poster assets to complete onboarding.
- Settings should expose a **simple light/dark/system appearance choice** for the unified Warm Archive identity. Do not regenerate Forest/Noble/Ink aesthetics in this redesign.

---

## 7. Layout and Responsive Rules

- **Phone-first** (target baseline roughly 360–412 dp wide), respect gesture-nav/status-bar and keyboard insets; never draw controls beneath system bars.
- Horizontal screen padding **16–20 dp**, narrow content column for prose; careful split layouts only when tablet width supports them.
- Content within sheet/dialog is scrollable if necessary but critical actions should stay accessible; avoid horizontal clipping at larger font sizes.
- For **600 dp+** widths, use a centered max-content width approximately **720–840 dp** for reflective layouts and responsive 2-column browsing for Library; never upscale phone cards to giant empty panels.
- One-column phone layout for substantial content. Horizontal carousels are permitted for known poster collections where clearly signaled; **no unexpected overall horizontal page scrolling**.
- Avoid symmetrical grids of three identical features. Use typographic hierarchy and controlled asymmetry instead.
- Touch target at least **48 × 48 dp** for Android (44 px as the absolute visual-prototype minimum).
- Respect Android system back behavior and screen reader semantics. A large image is never the sole navigation affordance.

---

## 8. Motion and Interaction

- Motion communicates **press, transition, selection, loading and saving**, never gamified excitement.
- Preferred transition: **140–220 ms** fade/slide for sheet entrance and page changes; pressed state ~**90–130 ms**. Modest spring for small tactile components (reference stiffness ≈ **100**, damping ≈ **20** for prototype; tune for Compose's actual animation APIs).
- Animate **opacity and translation/scale** where possible; avoid animated heights of long feeds and extensive blur filters. Never animate personal text while someone is reading it.
- Loading skeletons may use a **subtle repeated shimmer** until data arrives; no endless pulse on static buttons, chips or cards. No autoplay in Wrapped.
- Stagger a few hero elements only if it improves first impression; **never delay list usability** with 20 cascading row animations.
- Respect system **Remove animations / reduced motion** preferences, battery and low-end Android performance. Interactive feedback must still make sense with animation disabled.
- Save states: prevent double-submit, indicate progress and confirm success through normal screen feedback; no confetti, badges or novelty copy.

---

## 9. Content, Data Integrity and Privacy Boundaries

**Treat these as hard constraints when generating Stitch screens or translating them to Compose.**

- User-saved human content (notes, quotes, reflections, ratings, tags) must not be overwritten or fabricated by design, AI or metadata enrichment.
- A mockup may use illustrative work titles/posters, but any **displayed capability** must exist in the current app or be explicitly requested as a new feature.
- Do not invent voice notes, photo diaries, weather/location tracking, book purchase receipts, social profiles, followers, mood inference, semantic analysis of private notes, public feeds, cloud AI or subscription mechanics.
- Recommendation categories are **qualitative tiers**, not proven probabilities. Display a reason only if supplied by the actual recommendation evidence.
- Some works have no rating, no image, no current consumption, no completed date, or no quote: design graceful omissions rather than dummy values.
- Keep app **offline/local-first**. Screen layouts must behave well with cached metadata and missing network imagery.
- Nothing in this design document authorizes changes to Room entities, migration schemas, backup/import JSON or established business flows. Data changes require a separate explicit specification and backward-compatible migrations/import.

---

## 10. Banned Patterns / Anti-Generic Guardrails

**NEVER DO:**

- Generic Material 3 card stacking where every section is an identical rounded rectangle.
- Neon, blue/purple gradients, glassmorphism, excessive glass blur, glossy crypto-dashboard visuals or pure black backgrounds.
- More than one competing brand accent; olive/burgundy buttons beside terracotta primary actions.
- Fonts `Inter`, Times New Roman, Georgia or Garamond **as designed type choices**; use Newsreader + Work Sans. Fallback fonts are not aesthetic decisions.
- Three equal cards across a phone, tiny body text, excessive capitalization, giant empty top spacing, unreadable poster-overlaid text.
- Spurious stock photography, decorative people avatars or broken image links; use actual work art or elegant placeholders.
- Emoji decorations, fireworks, streaks, XP, achievements, social badges or meaningless metrics.
- Artificial numerical recommendation affinity (`99% match`), invented introspection (`14 notes about cosmic loneliness`), invented memories or generic AI marketing language (`unleash`, `seamless`, `next-gen`).
- Fake serial numbers, fictional profile identities or interface text that suggests nonexistent data-tracking functions.
- Auto-playing motion, floating CTA obstructions, nested scrolling traps, inaccessible text contrast or click targets under 48 dp.
- Literal reproduction of inconsistent export CSS; all colors and typography must come from the tables in this file.

---

## 11. Definition of Done — Visual Acceptance and Handoff

A Stitch generation or Compose implementation is visually ready when:

1. **Recognizable brand:** warm editorial atmosphere, one terracotta accent, Newsreader + Work Sans, artwork with breathing room.
2. **Theme consistency:** both light and dark use the **exact semantic tokens** above; no pseudo-dark cream screen.
3. **Information hierarchy:** personal cultural memories feel at least as important as provider metadata; no generic admin-dashboard styling.
4. **Fast core tasks:** progress and note/quote can be saved with few taps and without unsupported extra fields.
5. **Functional honesty:** every action maps to a current Memento flow; no fabricated data interpretation or recommendation probability.
6. **Full component states:** loading, empty, offline, error, long titles, no poster and accessibility scaling are accounted for.
7. **Implementation feasible:** scalable native Compose components, minimal one-off visuals, no required paid assets or complicated effects.
8. **Scope disciplined:** first validate master screens and common components; extend their patterns to all remaining flows without reintroducing multiple disconnected styles.

### Stitch execution instruction (append this to future screen prompts)

> Use `DESIGN.md` as the only source of colors, type scales, component shapes, navigation patterns and animation philosophy. Create a high-fidelity **native Android** screen for Memento in **Spanish**, with Warm Archive editorial personality, real supported product functionality, and appropriate Light or Dark tokens. Respect poster/cover authenticity, personal-memory priority, touch targets, fast capture, and accessibility. Do not invent features or recommendation percentages. If a previous Stitch screenshot contradicts `DESIGN.md`, use the screenshot for composition only and this document for tokens and behavior.

### Codex implementation note

Treat this as a **UI/UX contract**, not a request to alter product logic. Implement visual changes in Compose by related blocks, test on actual emulator in light/dark and at larger font scales, and compare screenshots against approved references. Preserve existing navigation and behavior where compatible; any intentional UX deviation must be explicit. Do not touch Room/backup/schema for a visual-only redesign.
