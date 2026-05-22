package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import com.example.data.api.WeatherApiClient
import com.example.ui.theme.WeatherThemeHelper
import kotlinx.coroutines.*

class WeatherWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            scope.launch {
                try {
                    // Try fetching Tokyo weather for widget refresh preview
                    val weather = WeatherApiClient.forecastService.getForecast(35.6762, 139.6503)
                    val current = weather.current
                    if (current != null) {
                        val spec = WeatherThemeHelper.getWeatherSpec(current.weatherCode)
                        val tempVal = Math.round(current.temperature2m)

                        withContext(Dispatchers.Main) {
                            val views = RemoteViews(context.packageName, R.layout.weather_widget_layout).apply {
                                setTextToBlankOrValue(R.id.widget_title, "Tokyo")
                                setTextToBlankOrValue(R.id.widget_desc, spec.description)
                                setTextToBlankOrValue(R.id.widget_temp, "$tempVal°C")
                                setTextToBlankOrValue(R.id.widget_emoji, spec.iconEmoji)
                            }
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                } catch (e: Exception) {
                    // Failsafe with static defaults if no internet
                    withContext(Dispatchers.Main) {
                        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout).apply {
                            setTextToBlankOrValue(R.id.widget_title, "Tokyo")
                            setTextToBlankOrValue(R.id.widget_desc, "Overcast")
                            setTextToBlankOrValue(R.id.widget_temp, "18°C")
                            setTextToBlankOrValue(R.id.widget_emoji, "⛅")
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel()
    }

    private fun RemoteViews.setTextToBlankOrValue(viewId: Int, value: String) {
        setTextViewText(viewId, value)
    }
}
