package com.tuempresa.inventariovial.scap.ui

import android.content.Context
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

class ScapController(context:Context,db:InventoryDatabase,scope:CoroutineScope) {
    private val _catalog=MutableStateFlow<ScapCatalog?>(null);val catalog=_catalog.asStateFlow()
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    private val _saveState=MutableStateFlow("Guardado local");val saveState=_saveState.asStateFlow()
    private val _pending=MutableStateFlow(0);val pending=_pending.asStateFlow()
    private val counter=AtomicInteger()
    private val actions=Channel<suspend (ScapRepository)->Unit>(Channel.UNLIMITED)
    private val failed=mutableListOf<suspend (ScapRepository)->Unit>()
    val selected=MutableStateFlow<String?>(null)
    private val _overrides=MutableStateFlow<Map<String,String>>(emptyMap());val overrides=_overrides.asStateFlow()
    private val ready=CompletableDeferred<ScapRepository>()
    val inspections=db.scapDao().observeInspections().stateIn(scope,SharingStarted.WhileSubscribed(5000),emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshot=selected.flatMapLatest {id->if(id==null) flowOf(null) else db.scapDao().observe(id)}
        .stateIn(scope,SharingStarted.WhileSubscribed(5000),null)
    init {
        scope.launch {
            try {val c=withContext(Dispatchers.IO){ScapCatalog.load(context)};_catalog.value=c;ready.complete(ScapRepository(db,c))}
            catch(e:Exception) {_error.value="No se pudo cargar el catálogo SCAP: ${e.message}";ready.completeExceptionally(e)}
        }
        scope.launch {
            for(action in actions) {
                try {action(ready.await())}
                catch(e:CancellationException){throw e}
                catch(e:Exception){failed+=action;_error.value=e.message ?: "No se pudo guardar localmente."}
                finally {
                    _pending.value=counter.decrementAndGet()
                    _saveState.value=if(failed.isNotEmpty()) "Error de guardado · reintentar" else if(counter.get()>0) "Guardando…" else "Guardado local"
                }
            }
        }
    }
    fun submit(action:suspend (ScapRepository)->Unit) {
        _pending.value=counter.incrementAndGet();_saveState.value="Guardando…"
        check(actions.trySend(action).isSuccess)
    }
    fun retry() {val list=failed.toList();failed.clear();_error.value=null;list.forEach(::submit)}
    fun clearError() {if(failed.isEmpty()) _error.value=null}
    fun select(id:String?) {selected.value=id}
    suspend fun savedSnapshot(id:String):ScapInspectionSnapshot {
        check(counter.get()==0 && failed.isEmpty()) {"Espera a que termine el guardado local."}
        return withContext(Dispatchers.IO){requireNotNull(ready.await().dao.snapshot(id)){"Inspección inexistente."}}
    }
    fun value(id:String,owner:String,key:String,s:ScapInspectionSnapshot):String =
        _overrides.value["$id/$owner/$key"] ?: s.values(owner)[key].orEmpty()
    fun edit(id:String,owner:String,key:String,value:String,source:String="MANUAL") {
        _overrides.value=_overrides.value+("$id/$owner/$key" to value)
        if(key=="category") _overrides.value=_overrides.value+("$id/$owner/type" to "")
        submit {it.setField(id,owner,key,value,source)}
    }
}
