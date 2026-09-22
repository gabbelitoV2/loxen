# Tier: ui

This file is SwiftUI. Translate it to Jetpack Compose with Material 3. Keep one @Composable function per SwiftUI View with the same name, taking the model objects it needs as parameters.

- View body -> @Composable fun. VStack -> Column. HStack -> Row. ZStack -> Box. Spacer -> Spacer(Modifier.weight(1f)) inside Row or Column. ScrollView -> Column with verticalScroll or LazyColumn. List and Form -> LazyColumn. Section -> a header Text followed by its items. ForEach -> items() in LazyColumn or a Kotlin loop.
- NavigationLink and NavigationStack -> a navigation callback parameter onNavigate: (String) -> Unit with the destination name; do not pull in navigation-compose inside this file.
- Text -> Text. Button -> Button or TextButton. Toggle -> Switch in a Row with a label. Picker -> a Row with an ExposedDropdownMenuBox. TextField and SecureField -> OutlinedTextField. Slider -> Slider. Stepper -> two IconButtons with a Text. ColorPicker -> TODO(). Image(systemName:) -> Icon(Icons.Default.<closest>, contentDescription = null). Divider -> HorizontalDivider. ProgressView -> CircularProgressIndicator or LinearProgressIndicator.
- .sheet -> ModalBottomSheet. .alert and .confirmationDialog -> AlertDialog. .toolbar -> TopAppBar actions. .navigationTitle -> the TopAppBar title.
- .padding -> Modifier.padding. .frame(width:height:) -> Modifier.size or Modifier.width and Modifier.height. .frame(maxWidth: .infinity) -> Modifier.fillMaxWidth(). .background -> Modifier.background. .foregroundColor -> color parameter. .font -> style parameter with MaterialTheme.typography. .cornerRadius -> Modifier.clip(RoundedCornerShape()). .opacity -> Modifier.alpha. .onTapGesture -> Modifier.clickable. .disabled -> enabled parameter. .hidden -> an if statement.
- @State -> remember { mutableStateOf() }. @Binding -> a value parameter plus an onChange lambda. @ObservedObject, @StateObject and @EnvironmentObject -> parameters whose StateFlow properties are read with collectAsState(). onAppear -> LaunchedEffect(Unit). onDisappear -> DisposableEffect. onChange(of:) -> LaunchedEffect(value).
- Color(...) -> androidx.compose.ui.graphics.Color. Custom colors named in the Swift code keep their names as top-level vals.
- Features with no Compose counterpart such as PhotosPicker, ShareLink, contextMenu previews and platform-specific pickers become TODO() and go in the unsupported list.
