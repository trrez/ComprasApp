package com.example.comprasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.comprasapp.db.AppDataBase
import com.example.comprasapp.db.Producto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Definición de las pantallas disponibles en la navegación
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AgregarProducto : Screen("Agregar_producto")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        lifecycleScope.launch(Dispatchers.IO) {
            val productoDao = AppDataBase.getInstance(this@MainActivity).productoDao()
            val cantProductos = productoDao.contar()
            if (cantProductos < 1){
                productoDao.insertar(Producto(0, "Pan", false))
                productoDao.insertar(Producto(0, "Queso", false))
                productoDao.insertar(Producto(0, "Jamon", false))
            }
        }

        setContent {
            val navController = rememberNavController()
            // Configuración de la navegación entre pantallas
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    // Composable para mostrar la lista de productos
                    ListaProductosUI(navController)
                }
                // Composable para agregar un nuevo producto
                composable(Screen.AgregarProducto.route) {
                    AgregarProducto(navController)
                }
            }
        }
    }
}


@Composable
fun ListaProductosUI(navController: NavHostController) {
    val contexto = LocalContext.current
    val (productos, setProductos) = remember { mutableStateOf(emptyList<Producto>()) }
    val texto = stringResource(id = R.string.texto)
    val add = stringResource(id = R.string.add)

    LaunchedEffect(productos) {
        withContext(Dispatchers.IO) {
            val dao = AppDataBase.getInstance(contexto).productoDao()
            val productosFromDB = dao.findAll()
            setProductos(productosFromDB)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }


            //En caso de que no se encuentren productos se va a mostrar un mensaje
            if (productos.isEmpty()) {
                item {
                    Text(
                        texto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(productos) { producto ->
                    ProductoItemUI(producto) {
                        val dao = AppDataBase.getInstance(contexto).productoDao()
                        val productosActualizados = dao.findAll()
                        setProductos(productosActualizados)
                    }
                }
            }
        }

        Button(
            onClick = {
                navController.navigate(Screen.AgregarProducto.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        ) {
            Text(add)
        }
    }
}



@Composable
fun ProductoItemUI(producto: Producto, onSave: () -> Unit = {} ){
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp)
    ) {
        //Cambiar a realizada o no realizada
        if(producto.realizada){
            Image(painter = painterResource(id = R.drawable.check),
                contentDescription = "Compra realizada",
                modifier = Modifier
                    .clickable {
                        alcanceCorrutina.launch(Dispatchers.IO) {
                            val dao = AppDataBase
                                .getInstance(contexto)
                                .productoDao()
                            producto.realizada = false
                            dao.actualizar(producto)
                            onSave()
                        }
                    }
                    .height(30.dp))
        } else {
            Image(painter = painterResource(id = R.drawable.no),
                contentDescription = "Compra no realizada",
                modifier = Modifier
                    .clickable {
                        alcanceCorrutina.launch(Dispatchers.IO) {
                            val dao = AppDataBase
                                .getInstance(contexto)
                                .productoDao()
                            producto.realizada = true
                            dao.actualizar(producto)
                            onSave()
                        }
                    }
                    .height(30.dp))
        }

        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = producto.producto,
            modifier = Modifier.weight(2f)
        )
        if (producto.id != 0){
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar Producto",
                modifier = Modifier.clickable {
                    alcanceCorrutina.launch(Dispatchers.IO) {
                        val dao = AppDataBase.getInstance(contexto).productoDao()
                        dao.eliminar(producto)
                        onSave()
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoProductoItemUI(onAgregarProducto: (String) -> Unit ){
    var nuevoProductoNombre by remember { mutableStateOf("") }
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()
    val nuevo = stringResource(id = R.string.nuevo)
    val add = stringResource(id = R.string.add)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 20.dp)
    ) {
        TextField(
            value = nuevoProductoNombre,
            onValueChange = {nuevoProductoNombre = it},
            label = { Text(nuevo)},
            modifier = Modifier
                .fillMaxWidth()
        )
        Button(
            onClick = {
                alcanceCorrutina.launch(Dispatchers.IO) {
                    val dao = AppDataBase
                        .getInstance(contexto)
                        .productoDao()
                    val nuevoProducto = Producto(0, nuevoProductoNombre, false)
                    dao.insertar(nuevoProducto)
                    onAgregarProducto(nuevoProductoNombre)
                    nuevoProductoNombre = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Agrega un espacio entre el TextField y el Button
        ) {
            Text(add)
        }
    }
}



@Composable
fun AgregarProducto(navController: NavHostController) {
    val contexto = LocalContext.current
    val (productos, setProductos) = remember { mutableStateOf(emptyList<Producto>()) }
    val regresar = stringResource(id = R.string.regresar)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        NuevoProductoItemUI {
            val dao = AppDataBase.getInstance(contexto).productoDao()
            val productosActualizados = dao.findAll()
            setProductos(productosActualizados)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Text(regresar)
        }
    }
}
