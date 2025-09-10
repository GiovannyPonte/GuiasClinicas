@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.gio.guiasclinicas.ui.components.workflow

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.text.ClickableText
import com.gio.guiasclinicas.ui.components.markdown.MarkdownText


/* ================== Constantes / utilidades ================== */
private const val LOG_TAG = "Workflow"

/* Banner de error en UI */
@Composable
private fun ErrorBanner(
    error: WorkflowError?,
    onDismiss: () -> Unit
) {
    if (error == null) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Se produjo un error", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("${error.where}: ${error.message}", style = MaterialTheme.typography.bodySmall)
            error.cause?.message?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp)); Text("Detalles: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    }
}

/* Canal de error para UI + Log */
private data class WorkflowError(val where: String, val message: String, val cause: Throwable? = null)
private fun logError(where: String, msg: String, e: Throwable? = null): WorkflowError {
    Log.e(LOG_TAG, "$where: $msg", e)
    return WorkflowError(where, msg, e)
}
private fun logWarn(where: String, msg: String) {
    Log.w(LOG_TAG, "$where: $msg")
}

/* ====== knobs de diseño (ajústalos a gusto) ====== */
private val OUTER_CARD_SHAPE = RoundedCornerShape(14.dp)          // contenedor “sección”
private val OUTER_PADDING_H = 0.dp                                // sangría lateral de la sección
private val OUTER_PADDING_V = 6.dp

private val STEP_CARD_SHAPE = RoundedCornerShape(12.dp)           // tarjeta de cada paso
private val STEP_PADDING_H = 10.dp
private val STEP_PADDING_V = 10.dp
/* ================================================= */

/* ============== Entrada pública de la sección ============== */
@Composable
fun WorkflowSectionView(
    relativePath: String,
    title: String,
    startButtonLabel: String,
    locale: String = "es",
    embedded: Boolean = true
) {
    val ctx = LocalContext.current
    var spec by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<WorkflowError?>(null) }

    LaunchedEffect(relativePath) {
        runCatching { readAssetJson(ctx, relativePath) }
            .onSuccess {
                spec = it
                Log.d(LOG_TAG, "Cargado OK: $relativePath  steps=${it.optJSONArray("steps")?.length() ?: 0}")
            }
            .onFailure {
                error = logError("Carga de JSON", it.message ?: "Error cargando $relativePath", it)
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = OUTER_CARD_SHAPE
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OUTER_PADDING_H, vertical = OUTER_PADDING_V),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ErrorBanner(error) { error = null }

            val showHostTitle = spec?.optJSONObject("ui")?.optBoolean("show_host_title", true) ?: true
            if (showHostTitle) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
            }

            when {
                error != null -> Text("No se pudo cargar el flujo.")
                spec == null -> Text("Cargando flujo…")
                else -> WorkflowHost(
                    spec = spec!!,
                    locale = locale,
                    startButtonLabel = startButtonLabel,
                    embedded = embedded
                )
            }
        }
    }
}

/* ======================= Host / Intérprete ======================= */
@Composable
private fun WorkflowHost(
    spec: JSONObject,
    locale: String,
    startButtonLabel: String,
    embedded: Boolean
) {
    // Scroll solo cuando NO está embebido en LazyColumn
    val maybeScroll = if (embedded) Modifier else Modifier.verticalScroll(rememberScrollState())

    val stepsArray: JSONArray = spec.optJSONArray("steps") ?: JSONArray()
    val stepMap: Map<String, JSONObject> = remember(stepsArray) { indexStepsById(stepsArray) }
    val variables: JSONArray = spec.optJSONArray("variables") ?: JSONArray()
    val varIndex: Map<String, JSONObject> = remember(variables.toString()) { indexVariablesById(variables) }
    val calculations: JSONArray = spec.optJSONArray("calculations") ?: JSONArray()
    val uiJson = spec.optJSONObject("ui")
    val showProgress = uiJson?.optBoolean("show_progress", false) ?: false
    val showRationaleDefault = uiJson?.optBoolean("show_rationale", true) ?: true

    // Estado de errores globales
    var lastError by remember { mutableStateOf<WorkflowError?>(null) }
    fun pushError(where: String, msg: String, e: Throwable? = null) { lastError = logError(where, msg, e) }

    LaunchedEffect(stepMap) {
        Log.d(LOG_TAG, "Workflow iniciado. steps=${stepMap.size} first=${stepMap.keys.firstOrNull()}")
    }

    // Estado de ejecución
    var started by rememberSaveable { mutableStateOf(false) }
    var currentId by rememberSaveable { mutableStateOf(stepMap.keys.firstOrNull() ?: "") }
    var history by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Entorno (inputs / steps / calc)
    val inputs = remember { mutableStateMapOf<String, Any?>() }
    val stepValues = remember { mutableStateMapOf<String, Any?>() }
    val calc = remember { mutableStateMapOf<String, Any?>() }

    // ===== Reinicio seguro (sin "__RESET__") =====
    fun resetWorkflow() {
        try {
            inputs.clear()
            stepValues.clear()
            calc.clear()
            history = emptyList()

            // Reaplica defaults declarados en "variables"
            for (i in 0 until variables.length()) {
                val v = variables.optJSONObject(i) ?: continue
                val id = v.optString("id")
                if (v.has("default")) inputs[id] = v.get("default")
            }

            currentId = stepMap.keys.firstOrNull() ?: ""
            Log.d(LOG_TAG, "RESET: ok → currentId=$currentId  defaults=${inputs.keys}")
        } catch (e: Throwable) {
            pushError("summary.reset", "No se pudo reiniciar", e)
        }
    }

    // Defaults de variables (al cargar por primera vez)
    LaunchedEffect(variables.toString()) {
        runCatching {
            for (i in 0 until variables.length()) {
                val v = variables.getJSONObject(i)
                val id = v.optString("id")
                if (inputs[id] == null && v.has("default")) inputs[id] = v.get("default")
            }
        }.onFailure { pushError("Variables", "Fallo al establecer defaults", it) }
    }

    // Cálculos globales reactivos
    LaunchedEffect(inputs.toMap(), stepValues.toMap()) {
        calc.clear()
        val env = EvalEnv(inputs = inputs, steps = stepValues, calc = calc)
        for (i in 0 until calculations.length()) {
            val c = calculations.optJSONObject(i) ?: continue
            val id = c.optString("id")
            val expr = c.opt("expr")
            if (c.optString("lang") == "jsonlogic" && id.isNotBlank()) {
                runCatching { evalJsonLogic(expr, env) }
                    .onSuccess { calc[id] = it }
                    .onFailure { e ->
                        calc[id] = null
                        pushError("Cálculo global", "Fallo evaluando '$id'", e)
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(maybeScroll)
            .padding(horizontal = OUTER_PADDING_H, vertical = OUTER_PADDING_V),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ErrorBanner(lastError) { lastError = null }

        if (!started) {
            Text(
                text = spec.optLoc("title", locale) ?: "Workflow",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            spec.optJSONObject("metadata")?.optString("last_review")?.let {
                Text("Última revisión: $it", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = {
                runCatching {
                    started = true
                    resetWorkflow() // ← inicia siempre limpio y con defaults
                }.onFailure { pushError("Inicio", "No se pudo iniciar el flujo", it) }
            }) { Text(startButtonLabel) }
        } else {
            if (showProgress) {
                val total = stepMap.size.coerceAtLeast(1)
                val visited = (history.toSet() + currentId).size
                LinearProgressIndicator(
                    progress = visited / total.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }

            val step = stepMap[currentId]
            if (step == null) {
                Text("Fin del flujo.")
            } else {
                StepView(
                    step = step,
                    locale = locale,
                    inputs = inputs,
                    stepValues = stepValues,
                    calc = calc,
                    showRationale = showRationaleDefault,
                    varSpecFor = { id -> varIndex[id] },
                    onInlineCalc = { id, value -> if (id != null) calc[id] = value },
                    onBack = {
                        if (history.isNotEmpty()) {
                            val prev = history.last()
                            history = history.dropLast(1)
                            Log.d(LOG_TAG, "onBack -> $prev")
                            currentId = prev
                        } else {
                            logWarn("Navegación", "No hay historial para volver")
                        }
                    },
                    onNext = { defaultNext, local ->
                        runCatching {
                            val env = EvalEnv(inputs, stepValues, calc).apply {
                                this.local = local
                                this.currentStepId = step.optString("id")
                            }
                            val goto = resolveNextId(step.opt("next"), env, default = defaultNext)
                            Log.d(LOG_TAG, "onNext: step=${step.optString("id")} default=$defaultNext resolved=$goto")
                            if (goto != null && stepMap.containsKey(goto)) {
                                history = history + currentId
                                currentId = goto
                            } else {
                                val msg = "Destino no resuelto o inexistente desde step=${step.optString("id")}"
                                pushError("Navegación", msg, null)
                            }
                        }.onFailure { e ->
                            pushError("Navegación", "Excepción al resolver siguiente paso", e)
                        }
                    },
                    onError = { where, msg, e -> pushError(where, msg, e) },
                    onReset = { resetWorkflow() } // ← botón Reiniciar en “summary” u otros
                )
            }
        }
    }
}


/* =============== UI de pasos (por tipo) ========================== */
@Composable
private fun StepView(
    step: JSONObject,
    locale: String,
    inputs: MutableMap<String, Any?>,
    stepValues: MutableMap<String, Any?>,
    calc: MutableMap<String, Any?>,
    showRationale: Boolean,
    varSpecFor: (String) -> JSONObject?,            // ← acceso a 'variables' por id (validaciones, etc.)
    onInlineCalc: (id: String?, value: Any?) -> Unit,
    onBack: () -> Unit,
    onNext: (defaultNext: String?, local: Any?) -> Unit,
    onError: (where: String, message: String, cause: Throwable?) -> Unit,
    onReset: () -> Unit
) {
    val type = step.optString("type")
    val title = step.optLoc("title", locale) ?: step.optString("id")
    val teach = step.optJSONArray("teach")

    var localError by remember { mutableStateOf<String?>(null) }
    var openHint by remember(step.optString("id")) { mutableStateOf<String?>(null) }


    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = STEP_CARD_SHAPE
    ) {
        ConsumeClicks {   // evita que el acordeón padre se cierre al tocar dentro
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = STEP_PADDING_H, vertical = STEP_PADDING_V),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                localError?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { localError = null }) { Text("OK") }
                        }
                    }
                }


                when (type) {

                    /* ===================== NUMBER ===================== */
                    "number" -> {
                        val idStep = step.optString("id")
                        val min = if (step.has("min")) step.optDouble("min") else Double.NaN
                        val max = if (step.has("max")) step.optDouble("max") else Double.NaN
                        val suffix = step.optString("suffix").takeIf { it.isNotBlank() }

                        var text by remember(idStep) {
                            mutableStateOf((stepValues[idStep]?.toString() ?: ""))
                        }
                        var err by remember(idStep) { mutableStateOf<String?>(null) }

                        fun validateAndStore(s: String) {
                            text = s
                            val parsed = parseDoubleLocaleAware(s)
                            // Guardar para cálculos reactivos
                            stepValues[idStep] = parsed
                            inputs[idStep] = parsed

                            err = null
                            if (s.isBlank()) { err = "Requerido"; return }
                            if (parsed == null) { err = "Valor no numérico"; return }
                            if (!min.isNaN() && parsed < min) { err = "Valor menor que el mínimo (${stripTrailingZeros(min)})"; return }
                            if (!max.isNaN() && parsed > max) { err = "Valor mayor que el máximo (${stripTrailingZeros(max)})"; return }
                        }

                        LaunchedEffect(Unit) { validateAndStore(text) }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { s -> validateAndStore(s) },
                                label = { Text(step.optLoc("title", locale) ?: idStep) },
                                singleLine = true,
                                isError = err != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                trailingIcon = { suffix?.let { Text(it, style = MaterialTheme.typography.bodySmall) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .disableStylusHandwriting()
                                    .showKeyboardOnFocus()
                            )
                            if (err != null) {
                                Text(
                                    err ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        if (showRationale) TeachBlock(teach, locale)
                        val ready = err == null && (stepValues[idStep] as? Double) != null
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(
                                enabled = ready,
                                onClick = { onNext(step.optStringNext(), stepValues[idStep]) }
                            ) { Text("Siguiente") }
                        }
                    }
                    /* ================================================== */


                    "info" -> {
                        val body = step.optLoc("body", locale)
                        val primary = step.optJSONObject("primary_action")
                        val primaryLabel = primary?.optLoc("label", locale) ?: "Continuar"
                        val primaryNext = primary?.optString("next")?.takeIf { it.isNotBlank() } ?: step.optStringNext()

                        // === cuerpo con scroll propio ===
                        val screen = androidx.compose.ui.platform.LocalConfiguration.current
                        // reserva ~220dp para título + paddings + botones; mínimo 240dp de lectura
                        val maxBodyHeight = (screen.screenHeightDp.dp - 220.dp).coerceAtLeast(240.dp)

                        Column(Modifier.fillMaxWidth()) {
                            // Título ya lo pinta arriba del when; aquí solo el cuerpo
                            if (!body.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 0.dp, max = maxBodyHeight)
                                        .verticalScroll(rememberScrollState())
                                        .padding(bottom = 8.dp)
                                ) {
                                    MarkdownText(
                                        body ?: "",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        onHintClick = { id -> openHint = id }   // ← ACTIVADOR DE “?”
                                    )

                                }
                            }
                            openHint?.let { HintDialog(step, it, locale) { openHint = null } }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onBack) { Text("Atrás") }
                                Button(onClick = { onNext(primaryNext, null) }) { Text(primaryLabel) }
                            }
                        }
                    }


                    "single_choice" -> {
                        val idStep = step.optString("id")
                        val options = step.optJSONArray("options") ?: JSONArray()
                        val sel = remember(idStep) { mutableStateOf(stepValues[idStep] as? String) } // null al entrar por primera vez
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0 until options.length()) {
                                val opt = try { options.getJSONObject(i) } catch (e: Throwable) {
                                    onError("single_choice", "Opción inválida en índice $i", e); null
                                }
                                if (opt != null) {
                                    val id = opt.optString("id")
                                    val labelBase = opt.optLoc("label", locale) ?: id
                                    val hintId = opt.optString("hint_calc").takeIf { it.isNotBlank() }
                                    val hintValue = hintId?.let { calc[it] }
                                    val label = if (hintValue != null) "$labelBase  [${hintValue}]" else labelBase
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = sel.value == id,
                                            onClick = {
                                                sel.value = id
                                                stepValues[idStep] = id
                                            }
                                        )
                                        Text(label, Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                        if (showRationale) TeachBlock(teach, locale)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(
                                enabled = sel.value != null, // no avanzar sin selección
                                onClick = { onNext(step.optStringNext(), sel.value) }
                            ) { Text("Siguiente") }
                        }
                    }

                    "checklist" -> {
                        val idStep = step.optString("id")
                        val items = step.optJSONArray("items") ?: JSONArray()
                        val current = remember(idStep) {
                            (stepValues[idStep] as? androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>)
                                ?: mutableStateMapOf<String, Boolean>().also { stepValues[idStep] = it }
                        }


                        for (i in 0 until items.length()) {
                            val itJ = try { items.getJSONObject(i) } catch (e: Throwable) {
                                onError("checklist", "Ítem inválido en índice $i", e); null
                            }
                            if (itJ != null) {
                                val id = itJ.optString("id")
                                val label = itJ.optLoc("label", locale) ?: id
                                val checked = current[id] ?: false
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { ok ->
                                            current[id] = ok
                                            // ⛔ NO cambies la referencia guardada en stepValues[idStep].
                                            // ✅ En su lugar, “dispara” el recálculo global con un contador (“tick”):
                                            val tickKey = "$idStep.__tick"
                                            val tickVal = (stepValues[tickKey] as? Int ?: 0) + 1
                                            stepValues[tickKey] = tickVal
                                        }
                                    )


                                    Text(label, Modifier.padding(start = 8.dp))
                                }
                            }
                        }

                        // Cálculo del paso (si lo hubiera)
                        step.optJSONObject("calc")?.let { c ->
                            if (c.optString("lang") == "jsonlogic") {
                                val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                    e.local = current      // ← en checklist la “local” es el mapa 'current'
                                    e.currentStepId = idStep
                                }
                                runCatching { evalJsonLogic(c.opt("expr"), env) }
                                    .onSuccess { res -> onInlineCalc(c.optString("id"), res) }
                                    .onFailure { e ->
                                        localError = "No se pudo calcular: ${c.optString("id")}"
                                        onError("checklist.calc", "Fallo en cálculo del paso", e)
                                    }
                            }
                        }

                        // calc_inline (nuevo, no intrusivo)
                        step.optJSONArray("calc_inline")?.let { arr ->
                            val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                e.local = current
                                e.currentStepId = idStep
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 0 until arr.length()) {
                                    val row = try { arr.getJSONObject(i) } catch (e: Throwable) {
                                        onError("checklist.calc_inline", "Fila inválida en índice $i", e); null
                                    }
                                    if (row != null) {
                                        val labelRow = row.optLoc("label", locale) ?: ""
                                        runCatching { evalJsonLogic(row.opt("value"), env) }
                                            .onSuccess { value -> Text("$labelRow: ${value.formatValue()}", style = MaterialTheme.typography.bodySmall) }
                                            .onFailure { e ->
                                                localError = "No se pudo calcular: $labelRow"
                                                onError("checklist.calc_inline", "Fallo en cálculo inline", e)
                                            }
                                    }
                                }
                            }
                        }

                        if (showRationale) TeachBlock(teach, locale)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(onClick = { onNext(step.optStringNext(), current) }) { Text("Siguiente") }
                        }
                    }

                    "score" -> {
                        val idStep = step.optString("id")
                        val items = step.optJSONArray("items") ?: JSONArray()
                        val values =
                            (stepValues[idStep] as? androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>)
                                ?: mutableStateMapOf<String, Boolean>().also { stepValues[idStep] = it }

                        var sum = 0.0
                        for (i in 0 until items.length()) {
                            val itJ = try { items.getJSONObject(i) } catch (e: Throwable) {
                                onError("score", "Ítem inválido en índice $i", e); null
                            }
                            if (itJ != null) {
                                val id = itJ.optString("id")
                                val label = itJ.optLoc("label", locale) ?: id
                                val pts = itJ.optDouble("points", 0.0)
                                val checked = values[id] ?: false
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = { ok -> values[id] = ok })
                                    Text("$label  (${stripTrailingZeros(pts)} pt)", Modifier.padding(start = 8.dp))
                                }
                                if (checked) sum += pts
                            }
                        }
                        stepValues["$idStep.sum"] = sum
                        Text(
                            "Puntaje: ${stripTrailingZeros(sum)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Cálculo del paso (si lo hubiera)
                        step.optJSONObject("calc")?.let { c ->
                            if (c.optString("lang") == "jsonlogic") {
                                val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                    e.local = values       // ← en score la “local” es el mapa 'values'
                                    e.currentStepId = idStep
                                }
                                runCatching { evalJsonLogic(c.opt("expr"), env) }
                                    .onSuccess { res -> onInlineCalc(c.optString("id"), res) }
                                    .onFailure { e ->
                                        localError = "No se pudo calcular: ${c.optString("id")}"
                                        onError("score.calc", "Fallo en cálculo del paso", e)
                                    }
                            }
                        }

                        // calc_inline (nuevo, no intrusivo)
                        step.optJSONArray("calc_inline")?.let { arr ->
                            val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                e.local = values
                                e.currentStepId = idStep
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 0 until arr.length()) {
                                    val row = try { arr.getJSONObject(i) } catch (e: Throwable) {
                                        onError("score.calc_inline", "Fila inválida en índice $i", e); null
                                    }
                                    if (row != null) {
                                        val labelRow = row.optLoc("label", locale) ?: ""
                                        runCatching { evalJsonLogic(row.opt("value"), env) }
                                            .onSuccess { value -> Text("$labelRow: ${value.formatValue()}", style = MaterialTheme.typography.bodySmall) }
                                            .onFailure { e ->
                                                localError = "No se pudo calcular: $labelRow"
                                                onError("score.calc_inline", "Fallo en cálculo inline", e)
                                            }
                                    }
                                }
                            }
                        }

                        step.optJSONArray("thresholds")?.let { ths ->
                            for (i in 0 until ths.length()) {
                                val txt = ths.optJSONObject(i)?.optLoc("label", locale)
                                if (txt != null) {
                                    AssistChip(onClick = { }, label = { Text(txt) })
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                        if (showRationale) TeachBlock(teach, locale)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(onClick = { onNext(step.optStringNext(), values) }) { Text("Siguiente") }
                        }
                    }

                    "form" -> {
                        val idStep = step.optString("id")
                        val fields = step.optJSONArray("fields") ?: JSONArray()
                        val current =
                            (stepValues[idStep] as? MutableMap<String, Any?>)
                                ?: mutableMapOf<String, Any?>().also { stepValues[idStep] = it }

                        // ====== pinta los campos ======
                        for (i in 0 until fields.length()) {
                            val f = try { fields.getJSONObject(i) } catch (e: Throwable) {
                                onError("form", "Campo inválido en índice $i", e); null
                            }
                            if (f != null) {
                                val id = f.optString("id")
                                val label = f.optLoc("label", locale) ?: id
                                when (f.optString("type")) {
                                    "number" -> {
                                        var tf by remember(id) { mutableStateOf(TextFieldValue((current[id]?.toString() ?: ""))) }
                                        OutlinedTextField(
                                            value = tf,
                                            onValueChange = {
                                                tf = it
                                                val parsed = parseDoubleLocaleAware(it.text)
                                                if (it.text.isNotBlank() && parsed == null) {
                                                    localError = "Valor no numérico. Usa punto o coma decimal."
                                                } else {
                                                    localError = null
                                                }
                                                current[id] = parsed
                                                inputs[id] = current[id]

                                                // Validación opcional (si existe en 'variables[id].validation')
                                                varSpecFor(id)?.optJSONObject("validation")?.let { vv ->
                                                    val min = if (vv.has("min")) vv.optDouble("min") else null
                                                    val max = if (vv.has("max")) vv.optDouble("max") else null
                                                    val dv = parsed
                                                    if (dv != null) {
                                                        if (min != null && !min.isNaN() && dv < min) localError = "Valor menor que el mínimo ($min)"
                                                        if (max != null && !max.isNaN() && dv > max) localError = "Valor mayor que el máximo ($max)"
                                                    }
                                                }
                                            },
                                            label = { Text(label) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            visualTransformation = VisualTransformation.None,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .disableStylusHandwriting()
                                                .showKeyboardOnFocus()
                                        )
                                    }
                                    "select" -> {
                                        val options = f.optJSONArray("options") ?: JSONArray()
                                        var expanded by remember(id) { mutableStateOf(false) }
                                        val opts = MutableList(options.length()) { idx -> options.optString(idx, "") }
                                        val initial = remember(id) {
                                            val cur = (current[id] as? String)?.takeIf { it in opts }
                                            val fromInputs = (inputs[id] as? String)?.takeIf { it in opts }
                                            cur ?: fromInputs ?: (if ("ug/mL" in opts) "ug/mL" else opts.firstOrNull().orEmpty())
                                        }
                                        var selected by remember(id) { mutableStateOf(initial) }
                                        LaunchedEffect(Unit) {
                                            if (inputs[id] != selected) inputs[id] = selected
                                            if (current[id] != selected) current[id] = selected
                                        }
                                        Box {
                                            OutlinedButton(onClick = { expanded = true }) {
                                                val shown = if (selected.isEmpty()) "—" else selected
                                                Text("$label: $shown")
                                            }
                                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                                for (opt in opts) {
                                                    DropdownMenuItem(
                                                        text = { Text(opt) },
                                                        onClick = {
                                                            selected = opt
                                                            expanded = false
                                                            current[id] = opt
                                                            inputs[id] = opt
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ====== EVALUAR EL CÁLCULO DEL PASO (p.ej., years_cut en years_ddimer) ======
                        step.optJSONObject("calc")?.let { c ->
                            if (c.optString("lang") == "jsonlogic") {
                                val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                    e.local = current
                                    e.currentStepId = idStep
                                }
                                runCatching { evalJsonLogic(c.opt("expr"), env) }
                                    .onSuccess { res -> onInlineCalc(c.optString("id"), res) }
                                    .onFailure { e ->
                                        localError = "No se pudo calcular: ${c.optString("id")}"
                                        onError("form.calc", "Fallo en cálculo del paso", e)
                                    }
                            }
                        }

                        // ====== CALC_INLINE DEL PASO ======
                        step.optJSONArray("calc_inline")?.let { arr ->
                            val env = EvalEnv(inputs, stepValues, calc).also { e ->
                                e.local = current
                                e.currentStepId = idStep
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 0 until arr.length()) {
                                    val row = try { arr.getJSONObject(i) } catch (e: Throwable) {
                                        onError("form.calc_inline", "Fila inválida en índice $i", e); null
                                    }
                                    if (row != null) {
                                        val labelRow = row.optLoc("label", locale) ?: ""
                                        runCatching { evalJsonLogic(row.opt("value"), env) }
                                            .onSuccess { value -> Text("$labelRow: ${value.formatValue()}", style = MaterialTheme.typography.bodySmall) }
                                            .onFailure { e ->
                                                localError = "No se pudo calcular: $labelRow"
                                                onError("form.calc_inline", "Fallo en cálculo inline", e)
                                            }
                                    }
                                }
                            }
                        }

                        if (showRationale) TeachBlock(teach, locale)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(onClick = { onNext(step.optStringNext(), current) }) { Text("Siguiente") }
                        }
                    }

                    "result" -> {
                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        val recRaw = step.optLoc("recommendation", locale) ?: step.optString("title")
                        // Guarda último result para potencial uso en summary
                        stepValues["last_result_id"] = step.optString("id")
                        stepValues["last_result_title"] = step.optLoc("title", locale) ?: step.optString("title")
                        stepValues["last_result_recommendation"] = recRaw

                        val sev = step.optString("severity")
                        val colors = when (sev) {
                            "success"   -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            "warning"   -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            "recommend" -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            "critical"  -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            // compat hacia atrás
                            "warn"      -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            "danger"    -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            else        -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                        val header = when (sev) {
                            "success" -> "Resultado"
                            "warning", "warn" -> "Aviso"
                            "recommend" -> "Recomendación"
                            "critical", "danger" -> "Importante"
                            else -> "Recomendación"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = colors,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(header, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                MarkdownText(
                                    recRaw,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    onHintClick = { id -> openHint = id }     // ← habilita “?”
                                )

                            }
                        }
                        openHint?.let { HintDialog(step, it, locale) { openHint = null } }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(recRaw)) }) { Text("Copiar") }
                            Button(onClick = { onNext(step.optStringNext(), null) }) { Text("Continuar") }
                        }
                    }

                    "summary" -> {
                        val tpl = step.optJSONObject("template")?.optString("es")
                        val env = EvalEnv(inputs, stepValues, calc)
                        val summary = runCatching {
                            if (!tpl.isNullOrBlank()) renderTemplate(tpl, env) else buildDefaultSummary(env)
                        }.getOrElse { e ->
                            onError("summary", "Error generando resumen", e)
                            buildDefaultSummary(env)
                        }
                        Text(summary, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(summary)) }) { Text("Copiar") }
                            Button(onClick = { onReset() }) { Text("Reiniciar") }
                        }
                    }

                    else -> {
                        Text("Paso no soportado: $type")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("Atrás") }
                            Button(onClick = { onNext(step.optStringNext(), null) }) { Text("Continuar") }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HintDialog(step: JSONObject, hintId: String, locale: String, onDismiss: () -> Unit) {
    val hint = step.optJSONObject("hints")?.optJSONObject(hintId)
    val title = hint?.optLoc("title", locale) ?: "Detalles"
    val text  = hint?.optLoc("text",  locale) ?: "—"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(title) },
        text  = { MarkdownText(text) }
    )
}

@Composable
private fun TeachBlock(teach: JSONArray?, locale: String) {
    if (teach == null || teach.length() == 0) return
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Por qué?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar" else "Ver") }
            }
            if (expanded) {
                for (i in 0 until teach.length()) {
                    val t = teach.optJSONObject(i)
                    if (t != null) {
                        t.optLoc("title", locale)?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                        t.optLoc("text", locale)?.let { MarkdownText(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

/* ==================== Utilidades JSON / Localización ==================== */
private fun readAssetJson(ctx: Context, path: String): JSONObject =
    ctx.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }

private fun JSONObject.optLoc(key: String, locale: String): String? {
    val raw = this.opt(key) ?: return null
    return when (raw) {
        is JSONObject -> raw.optString(locale, raw.optString("es", raw.optString("en", null)))
        is String -> raw
        else -> raw.toString()
    }
}

private fun JSONObject.optStringNext(): String? {
    val raw = this.opt("next")
    return if (raw is String) raw.trim().takeIf { it.isNotEmpty() } else null
}


/* Indexa pasos por id */
private fun indexStepsById(arr: JSONArray): Map<String, JSONObject> {
    val m = LinkedHashMap<String, JSONObject>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val id = o.optString("id").ifBlank { "step_$i" }
        m[id] = o
    }
    return m
}

/* Indexa variables por id (para validación/metadata) */
private fun indexVariablesById(arr: JSONArray): Map<String, JSONObject> {
    val m = LinkedHashMap<String, JSONObject>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i)
        if (o != null) {
            val rawId = o.optString("id")
            if (rawId.isNotBlank()) {
                m[rawId] = o
            }
        }
    }
    return m
}

/* ======================= Motor de transiciones ========================= */
private fun resolveNextId(next: Any?, env: EvalEnv, default: String?): String? {
    fun trimOrNull(s: String?): String? = s?.trim()?.takeIf { it.isNotEmpty() }

    return try {
        val resolved = when (next) {
            is String -> trimOrNull(next)
            is JSONObject -> {
                val cond = next.opt("when")
                val ok = if (cond == null) true else (evalJsonLogic(cond, env).truthy())
                if (ok) trimOrNull(next.optString("goto")) else null
            }
            is JSONArray -> {
                var dest: String? = null
                for (i in 0 until next.length()) {
                    val row = next.optJSONObject(i) ?: continue
                    val cond = row.opt("when")
                    val ok = if (cond == null) true else (evalJsonLogic(cond, env).truthy())
                    if (ok) { dest = trimOrNull(row.optString("goto")); if (dest != null) break }
                }
                dest
            }
            else -> null
        }
        resolved ?: trimOrNull(default)
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "resolveNextId error", e)
        trimOrNull(default)
    }
}


/* ======================= Evaluador JsonLogic (mínimo viable) ============ */
private data class EvalEnv(
    val inputs: Map<String, Any?>,
    val steps: Map<String, Any?>,
    val calc: Map<String, Any?>,
    var currentStepId: String? = null
) { var local: Any? = null }

private fun Any?.truthy(): Boolean = when (this) {
    null -> false
    is Boolean -> this
    is Number -> this.toDouble() != 0.0
    is String -> this.isNotEmpty()
    is Collection<*> -> this.isNotEmpty()
    is Map<*, *> -> this.isNotEmpty()
    else -> true
}


private fun Any?.asNumber(): Double? = when (this) {
    is Number -> toDouble()
    is String -> this.toDoubleOrNull()
    else -> null
}

private fun Any?.formatValue(): String = when (this) {
    null -> "—"
    is Double -> stripTrailingZeros(this)
    is Float  -> stripTrailingZeros(this.toDouble())
    else -> this.toString()
}

private fun stripTrailingZeros(x: Double): String =
    if (x % 1.0 == 0.0) x.toInt().toString() else "%.2f".format(x)

// REEMPLAZA tu evalJsonLogic por esta versión con cuerpo en bloque
private fun evalJsonLogic(expr: Any?, env: EvalEnv): Any? {
    return try {
        when (expr) {
            null -> null
            is Boolean, is Number, is String -> expr

            is JSONArray -> {
                val out = ArrayList<Any?>()
                for (i in 0 until expr.length()) out += evalJsonLogic(expr[i], env)
                out
            }

            is JSONObject -> {
                if (expr.length() != 1) return null
                val key = expr.keys().asSequence().first()
                val arg = expr.get(key)

                when (key) {
                    "var" -> resolveVar(arg, env)
                    "if"  -> evalIfChain(arg, env)
                    "cat" -> catOp(arg, env)

                    // dentro de evalJsonLogic(...) → when (key)
                    "in" -> {
                        val a = arg as? JSONArray ?: return false
                        val needle = evalJsonLogic(a.opt(0), env)
                        val hayRaw = evalJsonLogic(a.opt(1), env)

                        fun equalsLoose(x: Any?, y: Any?): Boolean {
                            return when {
                                x is Number && y is Number -> x.toDouble() == y.toDouble()
                                else -> (x?.toString() == y?.toString())
                            }
                        }

                        when (hayRaw) {
                            is JSONArray -> {
                                var found = false
                                for (j in 0 until hayRaw.length()) {
                                    if (equalsLoose(needle, hayRaw.opt(j))) { found = true; break }
                                }
                                found
                            }
                            is List<*> -> {
                                hayRaw.any { equalsLoose(needle, it) }
                            }
                            is String -> needle?.toString()?.let { hayRaw.contains(it) } ?: false
                            else -> false
                        }
                    }



                    // AND: devuelve el primer falsy o el último truthy
                    "and" -> {
                        val a = arg as? JSONArray ?: return null
                        var v: Any? = true
                        for (i in 0 until a.length()) {
                            v = evalJsonLogic(a[i], env)
                            if (!v.truthy()) return v
                        }
                        v
                    }

                    // OR: devuelve el primer truthy o el último evaluado
                    "or" -> {
                        val a = arg as? JSONArray ?: return false
                        var last: Any? = false
                        for (i in 0 until a.length()) {
                            val v = evalJsonLogic(a[i], env)
                            last = v
                            if (v.truthy()) return v
                        }
                        last
                    }

                    "!!" -> evalJsonLogic(arg, env).truthy()
                    "!"  -> !evalJsonLogic(arg, env).truthy()
                    "==" -> comp2(arg, env) { a, b -> a == b }
                    "!=" -> comp2(arg, env) { a, b -> a != b }
                    ">"  -> comp2(arg, env) { a, b -> (a.asNumber() ?: 0.0) >  (b.asNumber() ?: 0.0) }
                    ">=" -> comp2(arg, env) { a, b -> (a.asNumber() ?: 0.0) >= (b.asNumber() ?: 0.0) }
                    "<"  -> comp2(arg, env) { a, b -> (a.asNumber() ?: 0.0) <  (b.asNumber() ?: 0.0) }
                    "<=" -> comp2(arg, env) { a, b -> (a.asNumber() ?: 0.0) <= (b.asNumber() ?: 0.0) }
                    "+"  -> foldNum(arg, env) { a, b -> a + b }
                    "-"  -> foldNum(arg, env) { a, b -> a - b }
                    "*"  -> foldNum(arg, env) { a, b -> a * b }
                    "/"  -> foldNum(arg, env) { a, b -> if (b == 0.0) Double.NaN else a / b }

                    else -> { logWarn("JsonLogic", "Operador no soportado: $key"); null }
                }
            }

            else -> null
        }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "evalJsonLogic error", e)
        null
    }
}


private fun comp2(arg: Any?, env: EvalEnv, op: (Any?, Any?) -> Boolean): Boolean {
    return try {
        val arr = arg as? JSONArray ?: return false
        val a = evalJsonLogic(arr.opt(0), env)
        val b = evalJsonLogic(arr.opt(1), env)
        op(a, b)
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "comp2 error", e); false
    }
}

private fun foldNum(arg: Any?, env: EvalEnv, op: (Double, Double) -> Double): Double? {
    return try {
        val arr = arg as? JSONArray ?: return null
        var acc = evalJsonLogic(arr.opt(0), env).asNumber() ?: return null
        for (i in 1 until arr.length()) {
            val n = evalJsonLogic(arr.opt(i), env).asNumber() ?: return null
            acc = op(acc, n)
        }
        acc
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "foldNum error", e); null
    }
}

private fun resolveVar(arg: Any?, env: EvalEnv): Any? {
    return try {
        val p = (arg as? String) ?: return null
        if (p == "sum") { // Alias: sum del paso actual (para pasos type=score)
            val id = env.currentStepId ?: return null
            return env.steps["$id.sum"]
        }
        val parts = p.split('.')
        if (parts.isEmpty()) return null
        when (parts.first()) {
            "inputs" -> lookupNested(env.inputs, parts.drop(1))
            "calc"   -> lookupNested(env.calc, parts.drop(1))
            "steps"  -> {
                val key = parts.getOrNull(1) ?: return null
                val rest = parts.drop(2)
                val base = env.steps[key]
                if (base == null && env.steps.containsKey("$key.sum")) {
                    if (rest.isNotEmpty() && rest.first() == "sum") env.steps["$key.sum"] else null
                } else {
                    lookupNested(base, rest)
                }
            }
            "value" -> lookupNested(env.local, parts.drop(1))
            else -> {
                // Fallback para ids “desnudos”
                (env.inputs[p]) ?: (env.calc[p]) ?: (env.steps[p])
            }
        }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "resolveVar error", e); null
    }
}

private fun lookupNested(root: Any?, rest: List<String>): Any? {
    var cur: Any? = root
    for (k in rest) {
        cur = when (cur) {
            is Map<*, *> -> cur[k]
            is MutableMap<*, *> -> cur[k]
            is JSONObject -> (cur as JSONObject).opt(k)
            else -> return null
        }
    }
    return cur
}

/* ==================== Plantillas ============================ */
// Patrón compilado una vez
private val TEMPLATE_REGEX = Regex("\\{\\{\\s*([^}]+?)\\s*\\}\\}")

private fun renderTemplate(tpl: String, env: EvalEnv): String {
    return try {
        TEMPLATE_REGEX.replace(tpl) { m ->
            val key = m.groupValues[1].trim()
            val v = resolveVar(key, env)
                ?: env.inputs[key]
                ?: env.calc[key]
                ?: env.steps[key]
            v?.toString() ?: ""
        }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "renderTemplate error", e)
        "Resumen no disponible por error en plantilla."
    }
}

private fun buildDefaultSummary(env: EvalEnv): String {
    val sb = StringBuilder("Resumen:\n")
    sb.append("Entradas:\n")
    env.inputs.forEach { (k, v) -> sb.append(" • $k = ${v.formatValue()}\n") }
    if (env.calc.isNotEmpty()) {
        sb.append("Cálculos:\n")
        env.calc.forEach { (k, v) -> sb.append(" • $k = ${v.formatValue()}\n") }
    }
    return sb.toString()
}

/* ==================== Utilidad para consumir clicks ===================== */
@Composable
private fun ConsumeClicks(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.clickable(indication = null, interactionSource = src) { /* consume */ }
    ) { content() }
}

/* ==================== Stylus y teclado ===================== */
// Bloquea handwriting del stylus en este composable
private fun Modifier.disableStylusHandwriting(): Modifier =
    this.pointerInteropFilter { ev ->
        val stylus = (0 until ev.pointerCount).any { idx ->
            ev.getToolType(idx) == MotionEvent.TOOL_TYPE_STYLUS
        }
        // si hay stylus, consumimos -> el sistema no inicia handwriting
        stylus
    }

// Muestra el teclado en cuanto el campo gana foco
private fun Modifier.showKeyboardOnFocus(): Modifier = composed {
    val keyboard = LocalSoftwareKeyboardController.current
    this.onFocusChanged { state -> if (state.isFocused) keyboard?.show() }
}

/* ==================== Parsing numérico robusto ===================== */
private fun parseDoubleLocaleAware(text: String): Double? {
    val t = text.trim().replace(',', '.')
    return t.toDoubleOrNull()
}

/* ==================== Markdown ligero ===================== */





fun parseBold(s: String): AnnotatedString {
    // Ahora soporta **negrita**, *cursiva* y `código`
    val b = AnnotatedString.Builder()
    var i = 0
    var bold = false
    var italic = false
    var code = false

    fun pushBold() { b.pushStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
    fun pushItalic() { b.pushStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
    fun pushCode() { b.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace)) }

    fun popIf(cond: Boolean) { if (cond) b.pop() }

    while (i < s.length) {
        // **bold**
        if (!code && i + 1 < s.length && s[i] == '*' && s[i + 1] == '*') {
            if (bold) { b.pop(); bold = false } else { pushBold(); bold = true }
            i += 2; continue
        }
        // *italic*
        if (!code && s[i] == '*') {
            if (italic) { b.pop(); italic = false } else { pushItalic(); italic = true }
            i += 1; continue
        }
        // `code`
        if (s[i] == '`') {
            if (code) { b.pop(); code = false } else { pushCode(); code = true }
            i += 1; continue
        }

        b.append(s[i])
        i++
    }

    // cierra estilos abiertos por si acaso
    popIf(code); popIf(italic); popIf(bold)
    return b.toAnnotatedString()
}


private fun evalIfChain(arg: Any?, env: EvalEnv): Any? {
    val arr = arg as? JSONArray ?: return null
    var i = 0
    while (i + 1 < arr.length()) {
        val cond = evalJsonLogic(arr.opt(i), env).truthy()
        if (cond) return evalJsonLogic(arr.opt(i + 1), env)
        i += 2
    }
    // else final
    return if (i < arr.length()) evalJsonLogic(arr.opt(i), env) else null
}

// concat de cadenas/números
private fun catOp(arg: Any?, env: EvalEnv): String {
    val arr = arg as? JSONArray ?: return ""
    val sb = StringBuilder()
    for (i in 0 until arr.length()) {
        val v = evalJsonLogic(arr[i], env)
        when (v) {
            null -> {}
            is Double -> sb.append(stripTrailingZeros(v))
            is Float  -> sb.append(stripTrailingZeros(v.toDouble()))
            else      -> sb.append(v.toString())
        }
    }
    return sb.toString()
}
