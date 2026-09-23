# Tier: ui

This file is SwiftUI. Translate it to Jetpack Compose so that it looks and behaves exactly like the SwiftUI original on iOS. Keep one @Composable function per SwiftUI View with the same name. There is no MaterialTheme: never rely on Material defaults for sizes, colours, shapes or ripples.

## SwiftUI-style components

Package com.moblin.android.platform.swiftui is hand-written and mirrors SwiftUI. Use it instead of Material components:

- Form(title = "X", toolbar = { ... }) { ... } for Form + navigationTitle (it provides the NavigationStack). Never emit Scaffold or TopAppBar for a settings page.
- Section(header = "H", footer = "F") { rows }, or Section(footerContent = { ... }) { rows }. Section applies iOS row insets and separators, so do not pad plain rows.
- NavigationLink(destination = { D(args) }) { Label("T", systemImage = "s") } and NavigationLink("T") { D() }. Collect StateFlow values the destination needs inside the destination lambda.
- Toggle("T", isOn = v) { newValue -> ... } or Toggle("T", isOn = binding({ obj.prop }) { obj.prop = it }).
- Picker("T", selection = x, options = E.entries) { x = it }, with text = { ... } for custom labels and enabled for .disabled (see "Menus and pickers").
- Label("T", systemImage = "sf.name"), FormButton("T", destructive = true, centered = true) { }, FormSlider(value, onValueChange, modifier).
- Alert, ConfirmationDialog, Sheet and FullScreenCover for .alert, .confirmationDialog, .sheet and .fullScreenCover (see "Alerts, confirmation dialogs and sheets").
- ForEach, SwipeActions, ContextMenu, DeleteDisabled and MoveDisabled for editable lists, and Button, Menu and Divider for swipe actions and menus (see "Editable lists" and "Menus and pickers").
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
- `.fullScreenCover(isPresented: $x) { V() }` is `FullScreenCover(isPresented = x, onDismissRequest = { x = false }) { V() }`: it slides up over the whole screen on the system background, keeps the content inside the safe area, and the Android back button dismisses it. `.task { a() }` on V is `LaunchedEffect(Unit) { a() }` inside the cover.
- isPresented is the Boolean and onDismissRequest sets it to false: a `var x by remember { mutableStateOf(false) }`, a Boolean parameter with its change callback, or a flow (`isPresented = flag` from collectAsState, `onDismissRequest = { model.flag.value = false }`). A `MutableState<Boolean>` can be passed alone: `Alert("T", isPresented = state) { }`, `ConfirmationDialog("T", isPresented = state) { }`, `Sheet(isPresented = state) { }`.
- Always call the component (Alert, ConfirmationDialog, Sheet, FullScreenCover); never wrap it in `if (x)`, or it cannot animate out. It emits no layout, so it can stay where the Swift modifier is, also inside Section, Row or Column.
- The trailing lambda is the action builder (receiver DialogActions, not @Composable). Translate the Swift actions one to one, in the same order, keeping `if`, `else` and `for`: `Button("T") { a() }`, `Button("T", role = ButtonRole.destructive) { a() }`, `Button("Cancel", role = ButtonRole.cancel)`, `TextField("T", text = v) { v = it }` for `TextField("T", text: $v)`, and `SecureField` the same way. Titles are passed as in the Swift; the component localizes them.
- A button runs its action and then onDismissRequest, so never add code that sets isPresented to false (keep it only where the Swift action has it). Tapping outside a confirmation dialog, and the Android back button, run the cancel button. ConfirmationDialog adds the iOS Cancel button itself: only emit a cancel-role Button when the Swift has one. An Alert without buttons shows OK.
- `.confirmationDialog(..., presenting: data) { d in ... }` is a ConfirmationDialog with `isPresented = data != null`, `onDismissRequest = { data = null }`, and the builder reading data.
- UIAlertController(title:message:preferredStyle: .alert) with UIAlertAction and addTextField is an Alert (a nil title is `""`; style .cancel is ButtonRole.cancel, .destructive is ButtonRole.destructive, .default has no role). `.actionSheet` is a ConfirmationDialog with Visibility.visible. The handler code goes into the Button action.

## Editable lists

The components in com.moblin.android.platform.swiftui reproduce iOS list editing: swipe a row left to reveal its actions (tapping another row or scrolling closes it), a full swipe to delete, long-press and drag to reorder with the other rows sliding aside, and the long-press context menu. Never build these yourself: no SwipeToDismissBox, AnchoredDraggable, combinedClickable(onLongClick) for delete, drag handles, reorder libraries, or rows of delete and duplicate buttons under the row. DraggableItemPrefixView stays as the visual hint only.

- A SwiftUI ForEach inside a Form Section that has `.onDelete` or `.onMove`, or whose rows use `.swipeActions`, `.contextMenu`, `.contextMenuDeleteButton`, `.deleteDisabled` or `.moveDisabled`, is `ForEach(items, id = { it.id }, onDelete = { offsets -> ... }, onMove = { froms, to -> ... }) { item -> RowView(item) }`. Leave out onDelete and onMove when the Swift has none. `ForEach(items, id: \.self)` is `id = { it }`. Other ForEach loops stay `items.forEach { key(it.id) { ... } }`.
- `Section { List { ForEach ... } }` is the same as `Section { ForEach ... }`: drop the List, its rows are rows of the Section. Keep the ForEach at the same place among the other rows (a CreateButtonView after it stays after it).
- Pass the live list the Swift iterates (`database.streams`, `chat.filters`), not a copy; a Swift computed array (`filteredMessages()`) is passed as the Kotlin call. The ForEach recomposes the enclosing view after onDelete, onMove and every swipe or menu action, so never add extra state to refresh it.
- `.onDelete { offsets in X }` is `onDelete = { offsets -> X }` and `.onDelete(perform: f)` is `onDelete = { f(it) }`. offsets is IndexSet (a sorted Set<Int>). A Kotlin function that takes List<Int> gets `offsets.toList()`.
- `.onMove { froms, to in X }` is `onMove = { froms, to -> X }` with the SwiftUI meaning: froms is the IndexSet of source indices and to is the destination offset in the list before the move. `a.move(fromOffsets: froms, toOffset: to)` is `a.move(fromOffsets = froms, toOffset = to)` and `a.remove(atOffsets: offsets)` is `a.remove(atOffsets = offsets)`; always write these argument names. When the Kotlin property is a read-only List, assign `a = a.moving(fromOffsets = froms, toOffset = to)` or `a = a.removing(atOffsets = offsets)`.
- A ForEach bound to a variable with `.onDelete` in one branch only (`if c { list.onDelete(perform: f) } else { list }`) is one ForEach with `onDelete = if (c) { { offsets -> f(offsets) } } else { null }`; the same for onMove (`onMove = if (c) { { froms, to -> g(froms, to) } } else { null }`). Keep the inner braces: Kotlin reads `if (c) { offsets -> ... }` as a block, not a lambda, and does not compile it.
- `.swipeActions(edge: .trailing, allowsFullSwipe: false) { buttons }` on a row is `SwipeActions(edge = HorizontalEdge.trailing, allowsFullSwipe = false, actions = { buttons }) { row }`. Leave out edge and allowsFullSwipe when the Swift does (trailing and true). Translate the buttons one to one, in the same order and with the same if and else: `SwipeLeftToDeleteButtonView { a() }`, `SwipeLeftToDuplicateButtonView { a() }` or `Button(...)` (see "Buttons in swipe actions and menus"). The buttons are hidden until the row is swiped; never emit them next to the row.
- `.contextMenu { buttons }` is `ContextMenu(menu = { buttons }) { row }`. Keep `if isMac()` inside the menu: an empty menu shows nothing on long press, so the row can still be dragged. `.contextMenuDeleteButton(disabled: d) { a }` is `ContextMenuDeleteButton(disabled = d, action = { a }) { row }` from com.moblin.android.view.utils.
- `.deleteDisabled(x)` is `DeleteDisabled(x) { row }` and `.moveDisabled(x)` is `MoveDisabled(x) { row }`.
- The outermost Swift modifier becomes the outermost wrapper: `Row().deleteDisabled(d).swipeActions { A }.contextMenu { M }` is `ContextMenu(menu = { M }) { SwipeActions(actions = { A }) { DeleteDisabled(d) { Row() } } }`. The wrappers go where the Swift modifiers are, in the row's own view (a private ItemView) or in the ForEach body. They only act on a row of a Section; elsewhere they show the content unchanged.
- A row `HStack { DraggableItemPrefixView(); NavigationLink { D() } label: { Text(t) } }` is `NavigationLink(destination = { D() }) { DraggableItemTextView(name = t) }`, and `HStack { DraggableItemPrefixView(); Toggle(t, isOn: $x) }` is `Toggle(isOn = x, onChange = { ... }) { DraggableItemTextView(name = t) }`. A NavigationLink or Toggle is a row itself; inside a Row or FormRow of a Section it gets double insets and a row taller than 44 points.
- Example: `ForEach(database.streams) { stream in StreamItemView(stream: stream) }.onMove { froms, to in database.streams.move(fromOffsets: froms, toOffset: to) }` is `ForEach(database.streams, id = { it.id }, onMove = { froms, to -> database.streams.move(fromOffsets = froms, toOffset = to) }) { stream -> StreamItemView(stream = stream) }`.

## Buttons in swipe actions and menus

Button from com.moblin.android.platform.swiftui draws itself as a swipe action inside SwipeActions (tint colour, red for destructive, gray otherwise; icon only on rows lower than 91 points), as a menu item inside ContextMenu, Menu and Picker menus (title left, icon right, red when destructive), and elsewhere as a plain iOS button (accent colour, 0.2 alpha while pressed, a highlighted full-width row when it is a row of a Section).

- `Button { a() } label: { Label("T", systemImage: "i") }` is `Button(action = { a() }) { Label("T", systemImage = "i") }`. `Button("T") { a() }` is `Button("T") { a() }` and `Button("T", systemImage: "i") { a() }` is `Button("T", systemImage = "i") { a() }`. Titles are passed as in the Swift; the component localizes them.
- `role: .destructive` is `role = ButtonRole.destructive`, `.disabled(x)` is `enabled = !x`, and `.tint(.red)` on the Button is `CompositionLocalProvider(LocalTint provides formPalette().red) { Button(...) }` (`.blue` is formPalette().accent).
- A View whose body is one Button (SwipeLeftToDeleteButtonView, ContextMenuDuplicateButtonView) is translated the same way, so it works in swipe actions and menus.
- `Divider()` inside a menu is `Divider()`, and `Section("H") { buttons }` inside a menu is `Section(header = "H") { buttons }`: both draw the iOS menu group separator.

## Menus and pickers

Menus open the iOS pull-down menu anchored to the control, animate like iOS and support press, slide and release. Never use DropdownMenu, DropdownMenuItem, ExposedDropdownMenuBox, menuAnchor or OutlinedTextField for them.

- `Menu("T") { buttons }` is `Menu("T") { buttons }`, `Menu("T", systemImage: "i") { buttons }` is `Menu("T", systemImage = "i") { buttons }`, and `Menu { buttons } label: { L }` is `Menu(content = { buttons }) { L }`.
- A Picker in a Form, with or without `.pickerStyle(.menu)`, is `Picker("T", selection = x, options = E.entries, text = { it.toString() }) { x = it }`: the row shows the value with chevron.up.chevron.down and opens a menu with a checkmark on the selected option. `.labelsHidden()` or `Picker("", ...)` keeps the title `""`. Options drawn as `Image(systemName: o.image())` or `Label(t, systemImage: o.image())` add `systemImage = { it.image() }`.
- A Picker with `.pickerStyle(.menu)` outside a Form Section adds `pickerStyle = PickerStyle.menu`: it draws the compact accent button with the selected value (or image) and chevron.up.chevron.down. A Picker without `.pickerStyle` outside a Form (a stream overlay, a toolbar) is a menu on iOS as well, so it also gets `pickerStyle = PickerStyle.menu`. `.tint(.white)` or `.foregroundStyle(.primary)` on it is `CompositionLocalProvider(LocalTint provides Color.White) { Picker(...) }` or `LocalTint provides formPalette().label`.
- A Picker that shares a Form row with other views (`HStack { Label(...); Spacer(); Picker("", ...); Button("Send") }`) translates the HStack to `FormRow { ... }`: inside a FormRow the Picker draws only its value with chevron.up.chevron.down in the secondary colour and opens the same menu. Inside a plain Row it would draw a full row of its own and push the views after it out of the row.
- `.onChange(of: x)` after a Picker runs in the onChange lambda after the assignment, as for Toggle.

## Never leave an empty body

- Before writing TODO or Unit for a call into the model, look up the Kotlin extension function (fun Model.x in various/model) and call it. Port every call of a sequence.
- Private helpers that already exist in another file are imported, not stubbed.
- PhotosPicker is rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()).
