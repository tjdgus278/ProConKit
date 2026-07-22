package com.example.proconkit.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.proconkit.data.ControllerStateStore

class ProConWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    ControllerWidgetUpdater.update(
      context = context,
      manager = appWidgetManager,
      widgetIds = appWidgetIds,
      state = ControllerStateStore(context).load(),
    )
    ControllerWidgetUpdater.refreshAndUpdate(context)
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == ACTION_REFRESH) {
      ControllerWidgetUpdater.refreshAndUpdate(context)
      return
    }
    super.onReceive(context, intent)
  }

  companion object {
    const val ACTION_REFRESH = "com.example.proconkit.action.REFRESH_WIDGET"
  }
}
