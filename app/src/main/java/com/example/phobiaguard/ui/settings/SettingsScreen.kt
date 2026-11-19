package com.example.phobiaguard.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.phobiaguard.R
import com.example.phobiaguard.ui.icons.ChevronRightIcon
import com.example.phobiaguard.ui.icons.DeveloperIcon
import com.example.phobiaguard.ui.icons.HelpIcon
import com.example.phobiaguard.ui.icons.LockIcon
import com.example.phobiaguard.ui.icons.PersonIcon
import com.example.phobiaguard.ui.icons.SecurityIcon
import com.example.phobiaguard.ui.icons.SettingsIcon
import com.example.phobiaguard.ui.viewmodel.UserSettingsViewModel
import kotlin.math.roundToInt

private val screenBackground = Color(0xFF102C30)
private val panelBackground = Color(0xFF1D383A)
private val primaryText = Color(0xFFF3F0EA)
private val secondaryText = Color(0xFF9AA8A7)
private val accent = Color(0xFFD98A50)
private val dividerColor = Color(0xFF3A5556)
// Phobia choices available in the settings dropdown.
private val phobiaOptions = listOf("Spiders")

@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String {
    val packageManager = context.packageManager
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0L)
        )
    } else {
        packageManager.getPackageInfo(context.packageName, 0)
    }
    return packageInfo.versionName ?: "Unknown version"
}

private val settingsColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = screenBackground,
    background = screenBackground,
    onBackground = primaryText,
    surface = panelBackground,
    onSurface = primaryText,
    surfaceVariant = Color(0xFF294547),
    onSurfaceVariant = secondaryText,
    surfaceContainerLowest = screenBackground,
    surfaceContainerLow = panelBackground,
    surfaceContainer = panelBackground,
    surfaceContainerHigh = Color(0xFF284547),
    surfaceContainerHighest = Color(0xFF304C4E),
    outline = secondaryText,
    outlineVariant = dividerColor
)

// Defines the sections shown in the settings menu and the icon and label for each section.
private enum class SettingsSection(val label: String, val icon: ImageVector) {
    EditProfile("Edit profile", PersonIcon),
    DeveloperOptions("Developer options", DeveloperIcon),
    Privacy("Privacy", LockIcon),
    Security("Security", SecurityIcon),
    Settings("Settings", SettingsIcon),
    Help("Help", HelpIcon)
}

/**
 * Displays the user's profile and settings menu, and hosts the selected section's editor.
 * Settings and profile updates are sent to [viewModel], while image tests are run by
 * [modelTestViewModel].
 *
 * @param viewModel provides the current user settings and handles settings and profile updates.
 * @param modelTestViewModel runs spider detection on a selected image.
 * @param onNavigateBack callback invoked when the user selects the app bar's back action.
 * @param modifier modifier applied to the settings menu.
 */
@Composable
internal fun SettingsScreen(
    viewModel: UserSettingsViewModel,
    modelTestViewModel: SpiderModelTestViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val modelTestState by modelTestViewModel.state.collectAsStateWithLifecycle()
    val profilePictureError by viewModel.profilePictureError.collectAsStateWithLifecycle()
    var activeSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var name by rememberSaveable { mutableStateOf(settings.name) }
    var selectedPhobia by rememberSaveable { mutableStateOf(settings.selectedPhobia) }
    var intensity by rememberSaveable { mutableIntStateOf(settings.intensity) }
    var imagePickerError by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(modelTestViewModel::runModel) }
    val fallbackImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(modelTestViewModel::runModel) }
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(viewModel::saveProfilePicture) }
    val fallbackProfilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(viewModel::saveProfilePicture) }

    LaunchedEffect(settings) {
        if (activeSection == null) {
            name = settings.name
            selectedPhobia = settings.selectedPhobia
            intensity = settings.intensity
        }
    }

    fun openSection(section: SettingsSection) {
        name = settings.name
        selectedPhobia = settings.selectedPhobia
        intensity = settings.intensity
        activeSection = section
    }

    fun saveChanges() {
        viewModel.saveSettings(
            settings.copy(name = name, selectedPhobia = selectedPhobia, intensity = intensity)
        )
        activeSection = null
    }

    fun testModel() {
        modelTestViewModel.dismissResult()
        imagePickerError = null
        try {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            try {
                fallbackImagePicker.launch("image/*")
            } catch (_: ActivityNotFoundException) {
                imagePickerError = "This device does not have an image picker available."
            }
        }
    }

    fun changeProfilePicture() {
        imagePickerError = null
        try {
            profilePhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            try {
                fallbackProfilePhotoPicker.launch("image/*")
            } catch (_: ActivityNotFoundException) {
                imagePickerError = "This device does not have an image picker available."
            }
        }
    }

    MaterialTheme(colorScheme = settingsColorScheme) {
        SettingsMenuScreen(
            username = settings.name,
            profilePicture = settings.profilePicture,
            onNavigateBack = onNavigateBack,
            onSelectSection = ::openSection,
            modifier = modifier
        )

        activeSection?.let { section ->
            SettingsSectionSheet(
                section = section,
                name = name,
                profilePicture = settings.profilePicture,
                selectedPhobia = selectedPhobia,
                intensity = intensity,
                modelTestRunning = modelTestState == SpiderModelTestState.Running,
                onNameChange = { name = it },
                onChangeProfilePicture = ::changeProfilePicture,
                onPhobiaChange = { selectedPhobia = it },
                onIntensityChange = { intensity = it },
                onSave = ::saveChanges,
                onTestModel = ::testModel,
                onDismiss = { activeSection = null }
            )
        }

        when (val state = modelTestState) {
            SpiderModelTestState.Idle,
            SpiderModelTestState.Running -> Unit
            is SpiderModelTestState.Error -> AlertDialog(
                onDismissRequest = modelTestViewModel::dismissResult,
                title = { Text("Model test failed") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = modelTestViewModel::dismissResult) { Text("Close") }
                }
            )
            is SpiderModelTestState.Success -> SpiderModelResultDialog(
                result = state.result,
                onDismiss = modelTestViewModel::dismissResult
            )
        }

        imagePickerError?.let { message ->
            AlertDialog(
                onDismissRequest = { imagePickerError = null },
                title = { Text("Unable to open image picker") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { imagePickerError = null }) { Text("Close") }
                }
            )
        }

        profilePictureError?.let { message ->
            AlertDialog(
                onDismissRequest = viewModel::dismissProfilePictureError,
                title = { Text("Unable to save profile picture") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissProfilePictureError) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

/** Renders the scrollable settings menu with the user's profile and section choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMenuScreen(
    username: String,
    profilePicture: ByteArray?,
    onNavigateBack: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground)
            .clipToBounds()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = size.width * 0.36f,
                center = Offset(size.width * 1.13f, size.height * 0.12f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Go back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = primaryText,
                        navigationIconContentColor = primaryText
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val viewportHeight = constraints.maxHeight
                Layout(
                    content = {
                        BrandHeader(username, profilePicture)
                        SettingsMenuPanel(onSelectSection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) { children, constraints ->
                    val childConstraints = constraints.copy(
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                    val brand = children[0].measure(childConstraints)
                    val panel = children[1].measure(childConstraints)
                    val headerHeight = maxOf(brand.height, viewportHeight - panel.height)

                    layout(constraints.maxWidth, headerHeight + panel.height) {
                        brand.placeRelative(
                            x = (constraints.maxWidth - brand.width) / 2,
                            y = (headerHeight - brand.height) / 2
                        )
                        panel.placeRelative(0, headerHeight)
                    }
                }
            }
        }
    }
}

/** Shows the account avatar and display name above the settings menu. */
@Composable
private fun BrandHeader(username: String, profilePicture: ByteArray?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(profilePicture, Modifier.size(88.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = username,
            style = MaterialTheme.typography.headlineSmall,
            color = primaryText
        )
    }
}

/** Displays the decoded profile image, falling back to a person icon when unavailable. */
@Composable
private fun ProfileAvatar(profilePicture: ByteArray?, modifier: Modifier = Modifier) {
    val profileImage = remember(profilePicture) {
        profilePicture?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(panelBackground),
        contentAlignment = Alignment.Center
    ) {
        if (profileImage != null) {
            Image(
                bitmap = profileImage,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = PersonIcon,
                contentDescription = "Profile",
                tint = accent,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/** Groups settings destinations into the profile, developer, and general sections. */
@Composable
private fun SettingsMenuPanel(onSelectSection: (SettingsSection) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(panelBackground)
            .padding(top = 16.dp, bottom = 20.dp)
    ) {
        SettingsSection.entries.take(4).forEach { section ->
            SettingsMenuRow(section, onClick = { onSelectSection(section) })
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            color = dividerColor
        )
        SettingsSection.entries.drop(4).forEach { section ->
            SettingsMenuRow(section, onClick = { onSelectSection(section) })
        }
    }
}

/** Displays one clickable destination row in the settings menu. */
@Composable
private fun SettingsMenuRow(section: SettingsSection, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = section.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = primaryText
        )
        Icon(
            imageVector = ChevronRightIcon,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Presents the selected settings section in a bottom sheet on compact layouts or a side dialog on wide layouts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSectionSheet(
    section: SettingsSection,
    name: String,
    profilePicture: ByteArray?,
    selectedPhobia: String,
    intensity: Int,
    modelTestRunning: Boolean,
    onNameChange: (String) -> Unit,
    onChangeProfilePicture: () -> Unit,
    onPhobiaChange: (String) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onSave: () -> Unit,
    onTestModel: () -> Unit,
    onDismiss: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sheetContent: @Composable (Modifier) -> Unit = { contentModifier ->
            SettingsSectionContent(
                section = section,
                name = name,
                profilePicture = profilePicture,
                selectedPhobia = selectedPhobia,
                intensity = intensity,
                modelTestRunning = modelTestRunning,
                onNameChange = onNameChange,
                onChangeProfilePicture = onChangeProfilePicture,
                onPhobiaChange = onPhobiaChange,
                onIntensityChange = onIntensityChange,
                onSave = onSave,
                onTestModel = onTestModel,
                onDismiss = onDismiss,
                modifier = contentModifier
            )
        }

        if (maxWidth < 600.dp) {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = panelBackground,
                contentColor = primaryText,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                sheetContent(Modifier.fillMaxWidth().imePadding())
            }
        } else {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.56f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Close ${section.label}",
                                onClick = onDismiss
                            )
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(minOf(maxWidth * 0.75f, 480.dp))
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                        color = panelBackground,
                        contentColor = primaryText
                    ) {
                        sheetContent(
                            Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .imePadding()
                        )
                    }
                }
            }
        }
    }
}

/** Renders the controls and actions available for the selected settings section. */
@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    name: String,
    profilePicture: ByteArray?,
    selectedPhobia: String,
    intensity: Int,
    modelTestRunning: Boolean,
    onNameChange: (String) -> Unit,
    onChangeProfilePicture: () -> Unit,
    onPhobiaChange: (String) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onSave: () -> Unit,
    onTestModel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = primaryText
            )
            TextButton(onClick = onDismiss) { Text("Close") }
        }

        when (section) {
            SettingsSection.EditProfile -> {
                ProfileAvatar(
                    profilePicture = profilePicture,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Button(
                    onClick = onChangeProfilePicture,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change profile picture")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Save")
                }
            }
            SettingsSection.Settings -> {
                PhobiaDropdown(selectedPhobia, onPhobiaChange)
                IntensityControl(intensity, onIntensityChange)
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Save")
                }
            }
            SettingsSection.DeveloperOptions -> {
                Text(
                    text = "Choose an image to check it for spiders.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryText
                )
                Button(
                    onClick = onTestModel,
                    enabled = !modelTestRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Model")
                }
                if (modelTestRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Running spider detection…", color = secondaryText)
                }
            }
            SettingsSection.Privacy -> Text(
                text = "PhobiaGuard processes camera captures, selected test images, and screen frames " +
                    "on your device using its bundled model. The app does not upload these images " +
                    "to a server. Your name, selected phobia, intensity, and profile photo are saved " +
                    "in the app’s local database. Detection images and results are handled in " +
                    "memory and are not saved by the app. Android device backup settings may " +
                    "include app data.",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText
            )
            SettingsSection.Security -> Text(
                text = "PhobiaGuard relies on Android’s app sandbox to protect locally stored data. " +
                    "Camera access and screen capture require Android permission and your approval. " +
                    "The app does not add password protection or database encryption. Android " +
                    "backup settings may include app data; secure your device and review those " +
                    "settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText
            )
            SettingsSection.Help -> HelpContent()
        }
    }
}

/** Shows the app identity, a short description, and the project credits. */
@Composable
private fun HelpContent() {
    val context = LocalContext.current
    val versionName = remember(context) { appVersionName(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "PhobiaGuard app logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(112.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = primaryText
            )
            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
        }

        Text(
            text = "PhobiaGuard monitors your screen for spiders using on-device detection " +
                "and shows an alert when one is found. You can also test detection using an image.",
            style = MaterialTheme.typography.bodyLarge,
            color = secondaryText
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Credits",
                style = MaterialTheme.typography.titleMedium,
                color = primaryText
            )
            Text(
                text = "Daniel Pulido Ulvang\nJesper Ruud Soløst\nTilde Louise Gelbro Rørvik",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText
            )
        }
    }
}

/** Lets the user select one of the configured phobia options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhobiaDropdown(
    selectedPhobia: String,
    onPhobiaChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedPhobia,
            onValueChange = {},
            readOnly = true,
            label = { Text("Phobia") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            phobiaOptions.forEach { phobia ->
                DropdownMenuItem(
                    text = { Text(phobia) },
                    onClick = {
                        onPhobiaChange(phobia)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Displays the integer intensity setting on a slider with values from one through five. */
@Composable
private fun IntensityControl(
    intensity: Int,
    onIntensityChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Intensity: $intensity")
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onIntensityChange(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Previews the settings menu with a sample guest profile. */
@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme(colorScheme = settingsColorScheme) {
        SettingsMenuScreen(
            username = "Guest",
            profilePicture = null,
            onNavigateBack = {},
            onSelectSection = {}
        )
    }
}
