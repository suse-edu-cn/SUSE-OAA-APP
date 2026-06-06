import sys

file_path = "/Users/vincent/Desktop/SUSEOAA/composeApp/src/commonMain/kotlin/com/suseoaa/projectoaa/ui/screen/ailab/AiLabScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

imports = [
    "import androidx.compose.material.icons.filled.Folder",
    "import androidx.compose.foundation.layout.wrapContentHeight",
    "import androidx.compose.foundation.layout.heightIn",
    "import androidx.compose.foundation.lazy.items",
    "import androidx.compose.material.icons.filled.Delete"
]

for imp in imports:
    if imp not in content:
        content = content.replace("import androidx.compose.ui.window.Dialog", imp + "\nimport androidx.compose.ui.window.Dialog")

with open(file_path, "w") as f:
    f.write(content)

