package com.example.proconkit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.proconkit.MainActivity
import com.example.proconkit.R
import com.example.proconkit.data.ControllerStateStore
import com.proconkit.sdk.ControllerPowerState
import com.proconkit.sdk.ProControllerDiscovery
import com.proconkit.sdk.ProControllerState
import java.text.DateFormat
import java.util.Date

object ControllerWidgetUpdater {
  fun refreshAndUpdate(context: Context) {
    val state = ProControllerDiscovery.snapshot(context)
    ControllerStateStore(context).save(state)
    updateAll(context, state)
  }

  fun updateAll(context: Context, state: ProControllerState) {
    val manager = AppWidgetManager.getInstance(context)
    val component = ComponentName(context, ProConWidgetProvider::class.java)
    update(context, manager, manager.getAppWidgetIds(component), state)
  }

  fun update(
    context: Context,
    manager: AppWidgetManager,
    widgetIds: IntArray,
    state: ProControllerState,
  ) {
    widgetIds.forEach { widgetId ->
      manager.updateAppWidget(widgetId, createRemoteViews(context, state))
    }
  }

  private fun createRemoteViews(context: Context, state: ProControllerState): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.pro_controller_widget)
    val statusText =
      context.getString(if (state.isConnected) R.string.connected else R.string.disconnected)
    val statusColor =
      context.getColor(if (state.isConnected) R.color.widget_connected else R.color.widget_disconnected)
    val batteryText =
      state.batteryPercent?.let { context.getString(R.string.battery_percent, it) }
        ?: context.getString(R.string.battery_unavailable_short)
    val controllerName = state.deviceName ?: context.getString(R.string.pro_controller)
    val updatedTime =
      DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.lastUpdatedAtMillis))

    views.setTextViewText(R.id.widget_controller_name, controllerName)
    views.setTextViewText(R.id.widget_connection_status, statusText)
    views.setTextColor(R.id.widget_connection_status, statusColor)
    views.setTextViewText(R.id.widget_battery, batteryText)
    views.setTextViewText(R.id.widget_power_state, context.getString(state.powerState.stringResource()))
    views.setTextViewText(R.id.widget_updated_at, context.getString(R.string.updated_at, updatedTime))
    views.setViewVisibility(
      R.id.widget_battery_progress,
      if (state.batteryPercent != null) View.VISIBLE else View.INVISIBLE,
    )
    views.setProgressBar(R.id.widget_battery_progress, 100, state.batteryPercent ?: 0, false)

    val openAppIntent =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val refreshIntent =
      PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ProConWidgetProvider::class.java).setAction(ProConWidgetProvider.ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
    views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent)
    return views
  }

  private fun ControllerPowerState.stringResource(): Int =
    when (this) {
      ControllerPowerState.CHARGING -> R.string.charging
      ControllerPowerState.DISCHARGING -> R.string.on_battery
      ControllerPowerState.FULL -> R.string.fully_charged
      ControllerPowerState.NOT_CHARGING -> R.string.not_charging
      ControllerPowerState.UNKNOWN -> R.string.charging_unknown
      ControllerPowerState.UNAVAILABLE -> R.string.battery_api_unavailable
    }
}
