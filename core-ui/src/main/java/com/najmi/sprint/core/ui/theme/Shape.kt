package com.najmi.sprint.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Two-tier shape system as per Daily Ledger spec
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), // Fallback, not strictly defined in spec, using intermediate
    large = RoundedCornerShape(24.dp)
)
