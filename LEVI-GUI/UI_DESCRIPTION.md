# LEVI GUI - User Interface Description

## Application Overview

The LEVI GUI provides a modern, professional desktop interface for SNOMED CT translation validation and delta generation. The application follows a clean, organized layout with three main sections: Configuration, Jobs, and Results.

## Main Window Layout

```
┌──────────────────────────────────────────────────────────────────────┐
│  LEVI for SNOMED                                    [_] [□] [×]       │
├──────────────────────────────────────────────────────────────────────┤
│  File  Configuration  Help                                           │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─ Configuration ─────────────────────────────────────────────────┐│
│  │                                                                  ││
│  │  Database                                                        ││
│  │  ┌────────────────────────────────────────────────────────┐     ││
│  │  │ DB URL:       [jdbc:mysql://localhost:3306/snomed____] [Test]││
│  │  │ Username:     [root_______________________________]          ││
│  │  │ Password:     [••••••••••]                                   ││
│  │  └────────────────────────────────────────────────────────┘     ││
│  │                                                                  ││
│  │  Settings                                                        ││
│  │  Country Code: [CH ▼]  ☑ Eszett Transform  ☑ Regex Validation  ││
│  │                                                                  ││
│  │  File Paths                                                      ││
│  │  ┌────────────────────────────────────────────────────────┐     ││
│  │  │ Current File:  [/path/to/current.csv______________] [📁]    ││
│  │  │ Previous File: [/path/to/previous.csv_____________] [📁]    ││
│  │  │ Output Dir:    [/path/to/output___________________] [📁]    ││
│  │  └────────────────────────────────────────────────────────┘     ││
│  │                                                                  ││
│  │  [Save Config]  [Load Config]  [Restore Defaults]               ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                       │
│  ┌─ Jobs ───────────────────────────────────────────────────────────┐│
│  │                                                                  ││
│  │  [Translation Overview]  [New Descriptions]  [Inactivations]    ││
│  │  [Complete Delta]        [Eszett Check]      [Unpublished]      ││
│  │                                                                  ││
│  │  [▶ Start]  [⏹ Cancel]                                           ││
│  │                                                                  ││
│  │  ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░ 45%                                   ││
│  │  Status: Generating delta...                                     ││
│  │  Runtime: 00:01:23                                               ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                       │
│  ┌─ Results ────────────────────────────────────────────────────────┐│
│  │  [Statistics] [Log]                                              ││
│  │  ┌────────────────────────────────────────────────────────────┐ ││
│  │  │                                                             │ ││
│  │  │  === Delta Result: translate-delta ===                     │ ││
│  │  │                                                             │ ││
│  │  │  ✅ Additions:         1,234                                │ ││
│  │  │  🔄 Changes:             567                                │ ││
│  │  │  ❌ Inactivations:        89                                │ ││
│  │  │  ♻️  Reactivations:        12                                │ ││
│  │  │                                                             │ ││
│  │  │  ⚠️  Errors:               0                                │ ││
│  │  │  ⚠️  Warnings:             3                                │ ││
│  │  │                                                             │ ││
│  │  │  Runtime: 00:02:45                                          │ ││
│  │  │                                                             │ ││
│  │  └────────────────────────────────────────────────────────────┘ ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                       │
├──────────────────────────────────────────────────────────────────────┤
│  Status: Idle  |  DB: ● Connected  |  Last Job: translate-delta ✅   │
└──────────────────────────────────────────────────────────────────────┘
```

## Visual Elements

### Color Scheme
- **Primary Blue**: #2196F3 (headers, buttons, active elements)
- **Success Green**: #4CAF50 (progress bar, connection status, selected jobs)
- **Error Red**: #f44336 (validation errors, error counts)
- **Warning Orange**: #FF9800 (warnings)
- **Background**: #fafafa (light gray)
- **Text**: #333333 (dark gray)

### Typography
- **Main Font**: Segoe UI, Helvetica Neue, Arial (13px)
- **Headers**: Bold, 15px
- **Statistics**: Courier New, 14px (monospace for alignment)
- **Buttons**: Bold, 13px

### Visual Feedback

#### Job Buttons
- **Default**: Blue background (#2196F3)
- **Hover**: Darker blue (#1976D2)
- **Selected**: Green background (#4CAF50) ✓
- **Disabled**: Gray, 50% opacity

#### Validation States
- **Valid Field**: Normal white background, gray border
- **Invalid Field**: Red border (2px), red text below
- **Focused Field**: Blue border (2px)

#### Database Status
- **Connected**: Green dot (●) + "Connected"
- **Disconnected**: Red dot (●) + "Disconnected"
- **Testing**: Yellow dot (●) + "Testing..."

#### Progress Indicators
- **Idle**: Empty progress bar (0%)
- **Running**: Animated green bar with percentage
- **Complete**: Full bar (100%), 2 seconds then reset
- **Error**: Red bar

## Screen Sections

### 1. Configuration Section
**Purpose**: Manage all settings and file paths

**Elements**:
- Database connection fields with test button
- Settings toggles (country, eszett, regex)
- File/directory pickers with browse buttons
- Configuration action buttons

**Interactions**:
- Type or paste values into fields
- Click Browse buttons to select files/directories
- Click Test to verify database connection
- Real-time validation shows red borders for errors

### 2. Jobs Section
**Purpose**: Select and execute LEVI workflows

**Elements**:
- 6 job selection buttons in 2 rows
- Start/Cancel buttons
- Progress bar with percentage
- Status message label
- Runtime counter

**Interactions**:
- Click a job button (turns green when selected)
- Click Start to begin execution
- Click Cancel to abort running job
- Watch progress bar fill as job runs
- Monitor runtime in HH:MM:SS format

### 3. Results Section
**Purpose**: Display job outcomes and logs

**Tabs**:
1. **Statistics**: Summary of results with counts and emoji indicators
2. **Log**: Scrollable log output with clear and save buttons

**Elements**:
- Tab pane for switching views
- Monospace text area for statistics
- Scrollable log area
- Action buttons (Clear Log, Save Log)

**Interactions**:
- Switch between tabs to view different information
- Scroll through results
- Clear or save log to file

### 4. Status Bar
**Purpose**: Show application state at a glance

**Elements**:
- Overall status (Idle, Running, Complete)
- Database connection indicator (color-coded)
- Last job summary with result icon

**Visual Indicators**:
- Green ● = Database connected
- Red ● = Database disconnected
- ✅ = Job successful
- ❌ = Job failed

## Responsive Behavior

### Window Resizing
- **Minimum Size**: 1000×700 pixels
- **Preferred Size**: 1200×800 pixels
- **Maximum Size**: Unlimited
- **Scaling**: All sections scale proportionally

### Scrolling
- Configuration section: Fixed height, scrolls if needed
- Jobs section: Fixed, no scrolling
- Results section: Expands to fill available space
- Log area: Always scrollable

### Collapsible Sections
- Configuration: Collapsible TitledPane
- Jobs: Collapsible TitledPane
- Results: Collapsible TitledPane

Default: All sections expanded

## Accessibility

### Keyboard Navigation
- **Tab**: Move between fields
- **Enter**: Activate buttons
- **Space**: Toggle checkboxes
- **Arrow Keys**: Navigate combo boxes
- **Ctrl+S**: Save configuration
- **Ctrl+O**: Load configuration

### Screen Reader Support
- All fields have labels
- Buttons have descriptive text
- Status changes announced
- Error messages accessible

### High Contrast
- Clear visual distinction between elements
- Strong color contrast for text
- Visible focus indicators

## Tooltips

Every interactive element has a tooltip that appears on hover:

**Examples**:
- DB URL field: "JDBC URL to SNOMED database (e.g., jdbc:mysql://localhost:3306/snomed)"
- Country Code: "Country code for language RefSets (CH, AT, DE, FR, IT)"
- Eszett checkbox: "Transform ß to ss (recommended for CH/AT)"
- Test button: "Test database connection"
- Start button: "Start selected job"

## Animations

### Smooth Transitions
- Progress bar: Smooth fill animation
- Button hover: 200ms color transition
- Focus changes: 150ms border animation
- Tab switching: Fade in/out (100ms)

### Loading States
- Progress bar: Indeterminate mode for unknown duration
- Button states: Spinner during database test
- Status updates: Fade in when changed

## Error States

### Validation Errors
- **Visual**: Red 2px border around field
- **Message**: Red text below field
- **Tooltip**: Error details on hover
- **Button**: Start disabled until resolved

### Connection Errors
- **Dialog**: Modal alert with error message
- **Options**: [Retry] [Cancel] buttons
- **Status**: Red "Disconnected" in status bar

### Job Errors
- **Dialog**: Modal alert with error details
- **Log**: Full stack trace in log tab
- **Status**: Red ❌ in last job status

## Success States

### Successful Configuration
- **Visual**: Normal field appearance
- **Status**: Green "Connected" in status bar
- **Feedback**: Success dialog (optional)

### Successful Job
- **Progress**: 100% with green bar
- **Status**: "Complete" message
- **Results**: Statistics displayed
- **Last Job**: Green ✅ with job name

## Multi-Language Support

The interface adapts to the selected language:

**Supported Languages**:
- 🇩🇪 German (Deutsch) - Default for CH, AT, DE
- 🇬🇧 English - International default
- 🇫🇷 French (Français) - For FR
- 🇮🇹 Italian (Italiano) - For IT

**Language Selection**:
- Automatic based on country code selection
- All UI text, labels, buttons, and messages translated
- Error messages and tooltips localized

## Professional Features

### Auto-Save
- Configuration auto-saved on job start
- Last configuration loaded on startup
- No need to manually save every time

### Smart Defaults
- Country code: CH (Switzerland)
- Eszett transform: Enabled
- Regex check: Enabled
- Default paths: Empty (user must set)

### Input History
- Recently used file paths remembered
- File browser opens at last location
- Configuration history maintained

### Validation
- Real-time field validation
- Required field checking
- File existence verification
- Database connectivity testing
- Path accessibility checking

## Desktop Integration

### Native Look & Feel
- Adapts to operating system theme
- Uses system fonts
- Follows platform conventions
- Native file dialogs

### System Tray (Future)
- Minimize to system tray
- Background processing
- Notifications for job completion

### Drag & Drop (Future)
- Drag files onto window
- Drop to set current file
- Drop directory to set output path

## Conclusion

The LEVI GUI provides a comprehensive, user-friendly interface that makes SNOMED CT translation validation accessible to users without command-line experience. The clean design, clear visual feedback, and intuitive workflow guide users through the process step-by-step.

**Key Strengths**:
- ✅ Clear, organized layout
- ✅ Comprehensive visual feedback
- ✅ Real-time validation
- ✅ Progress tracking
- ✅ Multi-language support
- ✅ Professional appearance
- ✅ Accessible design
- ✅ Error prevention and recovery

**User Experience**: Modern, intuitive, and professional - suitable for both technical and non-technical users.
