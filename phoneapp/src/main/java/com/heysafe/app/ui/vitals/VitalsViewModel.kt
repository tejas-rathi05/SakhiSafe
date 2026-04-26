package com.heysafe.app.ui.vitals

import androidx.lifecycle.ViewModel
import com.heysafe.app.data.vitals.VitalsRepository

class VitalsViewModel(repo: VitalsRepository) : ViewModel() {
    val latest = repo.latest
    val history = repo.history
}
