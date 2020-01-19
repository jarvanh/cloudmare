package dev.jtsalva.cloudmare.api

import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import dev.jtsalva.cloudmare.CloudMareActivity
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.android.volley.Request as VolleyRequest
import com.android.volley.Response as JsonResponse

typealias ResponseCallback = (response: JSONObject?) -> Unit

@Suppress("UNUSED")
open class Request<R : Request<R>>(protected val context: CloudMareActivity) {

    companion object {
        const val CREATE = "create"
        const val GET = "get"
        const val LIST = "list"
        const val POST = "post"
        const val UPDATE = "update"
        const val DELETE = "delete"

        const val DIRECTION_ASCENDING = "asc"
        const val DIRECTION_DESCENDING = "desc"

        const val ORDER_STATUS = "status"
        const val ORDER_PRIORITY = "priority"

        const val CHARSET = "UTF-8"

        const val LOCAL_ERROR_CODE = -1
    }

    @Suppress("UNCHECKED_CAST")
    fun launch(block: suspend R.() -> Unit) = context.launch { block.invoke(this as R) }

    private val className = javaClass.simpleName

    protected var requestTAG: String = className
        set(method) {
            if (!(method == GET || method == LIST))
                cancelAll(method)

            field = "$className.$method"
        }

    protected inline fun urlParams(vararg params: Pair<String, Any>): String = params.run {
        var urlParamsString = "?"
        forEach { urlParamsString += "${it.first}=${it.second}&" }
        return urlParamsString.substring(0, urlParamsString.length - 1)
    }

    fun cancelAll(method: String) =
        RequestQueueSingleton(context).requestQueue.cancelAll("$className.$method")

    private inline fun handleError(error: VolleyError, callback: ResponseCallback) {
        Timber.e(error.message ?: error.toString())

        val response = error.networkResponse
        if (response != null) {
            try {
                val res = String(
                    response.data,
                    Charset.forName(HttpHeaderParser.parseCharset(response.headers, CHARSET))
                )
                Timber.e("Error Response: $res")
                callback(JSONObject(res))
            } catch (e: Throwable) {
                val failedResponse = Response.createWithErrors(
                    Response.Error(
                        code = LOCAL_ERROR_CODE,
                        message = "Something wen't wrong decoding error response"
                    )
                )

                callback(JSONObject(failedResponse))
                Timber.e(e)
            }
        } else {
            Timber.e(error.localizedMessage ?: "null error response")

            val failedResponse = Response.createWithErrors(
                Response.Error(
                    code = LOCAL_ERROR_CODE,
                    message = "Make sure you're connected to the internet"
                )
            )

            callback(JSONObject(failedResponse))
        }
    }

    private suspend inline fun <reified T : Response> send(method: Int, data: Any?, path: String) =
        suspendCoroutine<T> { cont ->
            val url = "$BASE_URL/$path"

            val callback: ResponseCallback = { response ->
                Timber.v(response.toString())
                cont.resume(
                    getAdapter(T::class).fromJson(response.toString())
                    ?: Response(success = false) as T
                )
            }

            RequestQueueSingleton.getInstance(context).addToRequestQueue(
                when (data) {
                    is JSONObject, null -> AuthenticatedJsonObjectRequest(
                        method,
                        url,
                        data as? JSONObject,
                        JsonResponse.Listener(callback),
                        JsonResponse.ErrorListener { error -> handleError(error, callback) }
                    )

                    is JSONArray -> AuthenticatedJsonArrayRequest(
                        method,
                        url,
                        data,
                        JsonResponse.Listener(callback),
                        JsonResponse.ErrorListener { error -> handleError(error, callback) }
                    )

                    else -> throw Exception("invalid request data type must be either JSONObject or JSONArray")

                }.apply { setTag(requestTAG) }
            )
        }

    internal suspend inline fun <reified T : Response> httpGet(path: String, data: Any?) =
        send<T>(VolleyRequest.Method.GET, data, path)

    internal suspend inline fun <reified T : Response> httpPatch(path: String, data: Any?) =
        send<T>(VolleyRequest.Method.PATCH, data, path)

    internal suspend inline fun <reified T : Response> httpPut(path: String, data: Any? = null) =
        send<T>(VolleyRequest.Method.PATCH, data, path)

    internal suspend inline fun <reified T : Response> httpPost(path: String, data: Any? = null) =
        send<T>(VolleyRequest.Method.POST, data, path)

    internal suspend inline fun <reified T : Response> httpDelete(path: String, data: Any? = null) =
        send<T>(VolleyRequest.Method.DELETE, data, path)

    internal suspend inline fun <reified T : Response> httpGet(path: String) = httpGet<T>(path, null)

    internal suspend inline fun <reified T : Response> httpPatch(path: String) = httpPatch<T>(path, null)

    internal suspend inline fun <reified T : Response> httpPut(path: String) = httpPut<T>(path, null)

    internal suspend inline fun <reified T : Response> httpPost(path: String) = httpPost<T>(path, null)

    internal suspend inline fun <reified T : Response> httpDelete(path: String) = httpDelete<T>(path, null)
}