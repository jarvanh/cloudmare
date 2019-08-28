package dev.jtsalva.cloudmare.viewmodel

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import dev.jtsalva.cloudmare.BR
import dev.jtsalva.cloudmare.CloudMareActivity
import dev.jtsalva.cloudmare.api.zonesettings.DevelopmentMode
import dev.jtsalva.cloudmare.api.zonesettings.DevelopmentModeRequest
import dev.jtsalva.cloudmare.api.zonesettings.SecurityLevel
import dev.jtsalva.cloudmare.api.zonesettings.SecurityLevelRequest

class DomainDashViewModel(
    private val context: CloudMareActivity,
    private val domainId: String
) : BaseObservable() {

    companion object {
        private const val TAG = "DomainDashViewModel"
    }

    private val data = object {
        var underAttackModeEnabled = false
        var developmentModeEnabled = false
    }

    @Bindable
    fun getUnderAttackModeEnabled(): Boolean {
        return data.underAttackModeEnabled
    }

    fun initUnderAttackModeEnabled(value: Boolean) {
        data.underAttackModeEnabled = value
        notifyPropertyChanged(BR.underAttackModeEnabled)
    }

    fun setUnderAttackModeEnabled(value: Boolean) = context.launch {
        val newSecurityLevelValue =
            if (value) SecurityLevel.UNDER_ATTACK
            else SecurityLevel.MEDIUM

        if (value != data.underAttackModeEnabled)
            SecurityLevelRequest(context).set(domainId, newSecurityLevelValue).let { response ->
                if (response.success) {
                    data.underAttackModeEnabled = value

                    Log.d(TAG, "Changed security level")
                }
                else {
                    data.underAttackModeEnabled = !value

                    context.dialog.error(
                        title = "Can't set under attack mode",
                        message = response.firstErrorMessage,
                        onAcknowledge = context::recreate)
                    Log.e(TAG, "Failed to change security level")
                }
                notifyPropertyChanged(BR.underAttackModeEnabled)
            }
    }

    @Bindable
    fun getDevelopmentModeEnabled(): Boolean = data.developmentModeEnabled

    fun initDevelopmentModeEnabled(value: Boolean) {
        data.developmentModeEnabled = value
        notifyPropertyChanged(BR.developmentModeEnabled)
    }

    fun setDevelopmentModeEnabled(value: Boolean) = context.launch {
        val newDevelopmentModeValue =
            if (value) DevelopmentMode.ON
            else DevelopmentMode.OFF

        if (value != data.developmentModeEnabled)
            DevelopmentModeRequest(context).set(domainId, newDevelopmentModeValue).let { response ->
                if (response.success) {
                    data.developmentModeEnabled = value

                    Log.d(TAG, "Changed development mode")
                } else {
                    data.developmentModeEnabled = !value

                    context.dialog.error(title = "Can't set development mode", message = response.firstErrorMessage)
                    Log.e(TAG, "Failed to change development mode")
                }
                notifyPropertyChanged(BR.developmentModeEnabled)
            }
    }

}