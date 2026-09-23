# Tier: ui

This file is SwiftUI. Translate it to Jetpack Compose so that it looks and behaves exactly like the SwiftUI original on iOS. Keep one @Composable function per SwiftUI View with the same name. There is no MaterialTheme: never rely on Material defaults for sizes, colours, shapes or ripples.

## SwiftUI-style components

Package com.moblin.android.platform.swiftui is hand-written and mirrors SwiftUI. Use it instead of Material components:

- Form(title = "X", toolbar = { ... }) { ... } for Form + navigationTitle (it provides the NavigationStack). Never emit Scaffold or TopAppBar for a settings page.
- Section(header = "H", footer = "F") { rows }, or Section(footerContent = { ... }) { rows }. Section applies iOS row insets and separators, so do not pad plain rows.
- NavigationLink(destination = { D(args) }) { Label("T", systemImage = "s") } and NavigationLink("T") { D() }. Collect StateFlow values the destination needs inside the destination lambda.
- Toggle("T", isOn = v) { newValue -> ... } or Toggle("T", isOn = binding({ obj.prop }) { obj.prop = it }).
- Picker("T", selection = x, options = E.entries) { x = it }, with text = { ... } for custom labels and enabled for .disabled.
- Label("T", systemImage = "sf.name"), FormButton("T", destructive = true, centered = true) { }, FormSlider(value, onValueChange, modifier).
- Alert, ConfirmationDialog and Sheet for .alert, .confirmationDialog and .sheet (see "Alerts, confirmation dialogs and sheets").
- formPalette() holds the iOS system colours (accent, red, gray, secondaryLabel); LocalTint replaces .tint(); binding(get, set) replaces @Binding to plain properties.
- com.moblin.android.platform.SystemImage(name, fontSize, modifier, tint) and systemImage(name) for Image(systemName:). Never pick Material icons yourself.
- Image("AssetName") and Image(variable) for asset catalog images -> AssetImage(name = "AssetName", modifier, contentScale) or rememberAssetImage(name) from com.moblin.android.platform.swiftui (both read assets/Assets through com.moblin.android.platform.Bundle.image). Never use resources.getIdentifier, painterResource or R.drawable: the app has no drawable resources and a missing id crashes.

## Layout

- SwiftUI modifiers apply inside-out and Compose modifiers outside-in: reverse the modifier list. `.frame(w, h).padding(p).background(c).cornerRadius(r)` becomes `Modifier.clip(RoundedCornerShape(r)).background(c).padding(p).size(w, h)`.
- VStack and HStack without a spacing argument use 8.dp: `Arrangement.spacedBy(8.dp)`. Only an explicit `spacing: 0` means no arrangement.
- VStack centres horizontally, HStack centres vertically and ZStack centres: `Column(horizontalAlignment = Alignment.CenterHorizontally)`, `Row(verticalAlignment = Alignment.CenterVertically)`, `Box(contentAlignment = Alignment.Center)`, unless the Swift passes another alignment.
- `.padding()` without a length is 16.dp. Negative padding is illegal in Compose: use Modifier.offset or drawBehind.
- `.frame(maxWidth: .infinity)` is fillMaxWidth(). `.frame(maxWidth: a)` is `widthIn(max = a).fillMaxWidth()`. `.frame(width:height:)` on Text is a centred Box around the Text.
- A Spacer or flexible child inside VStack/HStack gets Modifier.weight(1f). A SwiftUI ScrollView fills the proposed size: add fillMaxSize/fillMaxHeight to the Compose scroll container.
- `.overlay { Child }` is `Box { View(); Box(Modifier.matchParentSize()) { Child() } }`. Content that SwiftUI lets spill out of a small frame needs wrapContentHeight(unbounded = true) or requiredSize.
- `.zIndex(n)` is Modifier.zIndex(n). `.rotationEffect(a).offset(x, y)` is `Modifier.offset(x, y).rotate(a)`.
- Canvas gets Modifier.fillMaxSize(), and point values used for drawing are converted with `n.dp.toPx()`.
- `EllipticalGradient(startRadiusFraction: a, endRadiusFraction: b)` is `Brush.radialGradient(0f to c0, 2*a to c0, 2*b to c1)`.

## Text

- Default text is 17.sp (body). Text styles: largeTitle 34, title 28, title2 22, title3 20, headline 17 semibold, body 17, callout 16, subheadline 15, footnote 13, caption 12, caption2 11.
- `.font(f)` on a parent applies to child Text: wrap the child in `CompositionLocalProvider(LocalTextStyle provides f)`.
- `.minimumScaleFactor(f)` needs a shrink-on-overflow Text (onTextLayout lowers the size while hasVisualOverflow, down to f); maxLines plus ellipsis is not the same.
- `.fixedSize()` on Text is `softWrap = false` with `Modifier.wrapContentSize(unbounded = true)`.
- Text("literal") is `Text(localized("literal"))`.

## Colours

- iOS system colours, not Material: red 0xFFFF3B30, orange 0xFFFF9500, yellow 0xFFFFCC00, green 0xFF34C759, blue 0xFF007AFF, gray 0xFF8E8E93, secondary 0x99EBEBF5 on dark.
- `rgbColor.color()` from com.moblin.android.common.various keeps opacity; never rebuild a Color from red, green and blue.

## State and events

- A @Published property that the body reads is observed with `val x by flow.collectAsState()`. A Model helper that reads flows internally (model.isShowingStatusX(), model.isStreamConfigured()) is evaluated after collecting those flows, so the composable recomposes.
- `.onChange(of: x) { }` runs only when x changes, not on first composition: keep the previous value in remember and compare, or run the side effect in the setter that changes x.
- `.onAppear { a() }.onDisappear { b() }` is `DisposableEffect(Unit) { a(); onDispose { b() } }`. Callbacks read `flow.value` at call time.
- `.onTapGesture` is `pointerInput(keys) { detectTapGestures(onTap = ...) }`, and a plain SwiftUI Button is `clickable(interactionSource, indication = null)` with alpha 0.2 while pressed. Never add Material ripples, IconButton or filled Buttons.
- `.allowsHitTesting(false)` means no pointer modifier at all; never consume events to emulate it. `.opacity(flag ? 1 : 0)` on interactive content also removes it from hit testing.
- ForEach over Identifiable items keys by id: `key(item.id)`.
- Enum comparisons use the enum entry, never toString() or a name lookup.
- `UIViewRepresentable` is AndroidView(factory, modifier, update, onRelease); a shared View is removed from its previous parent before it is added.
- `if #available(iOS 26, *) { glassEffect ... } else { classic }`: translate the else branch.

## Alerts, confirmation dialogs and sheets

Never use Material AlertDialog, BasicAlertDialog, ModalBottomSheet, DropdownMenu or a hand-made Dialog for these. The components in com.moblin.android.platform.swiftui draw them as iOS does and animate them in and out.

- `.alert("T", isPresented: $x) { actions } message: { Text("M") }` is `Alert("T", isPresented = x, onDismissRequest = { x = false }, message = "M") { actions }`.
- `.confirmationDialog("T", isPresented: $x, titleVisibility: .visible) { actions } message: { Text("M") }` is `ConfirmationDialog("T", isPresented = x, onDismissRequest = { x = false }, titleVisibility = Visibility.visible, message = "M") { actions }`. Leave out titleVisibility when the Swift does (the title is then hidden, as on iOS), and message when there is none. Keep `""` titles.
- `.sheet(isPresented: $x) { V() }` and `.popover(isPresented: $x) { V() }` are `Sheet(isPresented = x, onDismissRequest = { x = false }) { V() }`. `.sheet(item: $i) { V(it) }` is `Sheet(isPresented = i != null, onDismissRequest = { i = null }) { i?.let { V(it) } }`.
- isPresented is the Boolean and onDismissRequest sets it to false: a `var x by remember { mutableStateOf(false) }`, a Boolean parameter with its change callback, or a flow (`isPresented = flag` from collectAsState, `onDismissRequest = { model.flag.value = false }`). A `MutableState<Boolean>` can be passed alone: `Alert("T", isPresented = state) { }`, `ConfirmationDialog("T", isPresented = state) { }`, `Sheet(isPresented = state) { }`.
- Always call the component; never wrap it in `if (x)`, or it cannot animate out. It emits no layout, so it can stay where the Swift modifier is, also inside Section, Row or Column.
- The trailing lambda is the action builder (receiver DialogActions, not @Composable). Translate the Swift actions one to one, in the same order, keeping `if`, `else` and `for`: `Button("T") { a() }`, `Button("T", role = ButtonRole.destructive) { a() }`, `Button("Cancel", role = ButtonRole.cancel)`, `TextField("T", text = v) { v = it }` for `TextField("T", text: $v)`, and `SecureField` the same way. Titles are passed as in the Swift; the component localizes them.
- A button runs its action and then onDismissRequest, so never add code that sets isPresented to false (keep it only where the Swift action has it). Tapping outside a confirmation dialog, and the Android back button, run the cancel button. ConfirmationDialog adds the iOS Cancel button itself: only emit a cancel-role Button when the Swift has one. An Alert without buttons shows OK.
- `.confirmationDialog(..., presenting: data) { d in ... }` is a ConfirmationDialog with `isPresented = data != null`, `onDismissRequest = { data = null }`, and the builder reading data.
- UIAlertController(title:message:preferredStyle: .alert) with UIAlertAction and addTextField is an Alert (a nil title is `""`; style .cancel is ButtonRole.cancel, .destructive is ButtonRole.destructive, .default has no role). `.actionSheet` is a ConfirmationDialog with Visibility.visible. The handler code goes into the Button action.

## Never leave an empty body

- Before writing TODO or Unit for a call into the model, look up the Kotlin extension function (fun Model.x in various/model) and call it. Port every call of a sequence.
- Private helpers that already exist in another file are imported, not stubbed.
- PhotosPicker is rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()).
