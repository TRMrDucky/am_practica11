package mx.itson.edu.practica11

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var txtid: EditText
    private lateinit var txtnom: EditText
    private lateinit var btnbus: Button
    private lateinit var btnmod: Button
    private lateinit var btnreg: Button
    private lateinit var btneli: Button
    private lateinit var lvDatos: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val root: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        txtid = findViewById(R.id.txtid)
        txtnom = findViewById(R.id.txtnom)
        btnbus = findViewById(R.id.btnbus)
        btnmod = findViewById(R.id.btnmod)
        btnreg = findViewById(R.id.btnreg)
        btneli = findViewById(R.id.btneli)
        lvDatos = findViewById(R.id.lvDatos)
        botonRegistrar()
        listarLuchadores()
        botonBuscar()
        botonModificar()
        botonEliminar()
    }

    private fun botonRegistrar() {
        btnreg.setOnClickListener {
            if (txtid.text.toString().trim().isEmpty() || txtnom.text.toString().trim().isEmpty()) {
                ocultarTeclado()
                Toast.makeText(this, "Complete Los Campos Faltantes!!", Toast.LENGTH_SHORT).show()
            } else {
                val id = txtid.text.toString().toInt()
                val nom = txtnom.text.toString().trim()
                val db = FirebaseDatabase.getInstance()
                val dbref = db.getReference(Luchador::class.java.simpleName)
                dbref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val luc = Luchador(id, nom)
                        agregarLuchador(luc)
                            .addOnSuccessListener {
                                ocultarTeclado()
                                Toast.makeText(this@MainActivity, "Luchador Agregado Correctamente!!", Toast.LENGTH_SHORT).show()
                                txtid.setText("")
                                txtnom.setText("")
                                txtid.requestFocus()
                                listarLuchadores()
                            }
                            .addOnFailureListener {
                                ocultarTeclado()
                                Toast.makeText(this@MainActivity, "Error Al Intentar Agregar Luchador!!", Toast.LENGTH_SHORT).show()
                                txtid.setText("")
                                txtnom.setText("")
                                txtid.requestFocus()
                            }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun agregarLuchador(luc: Luchador): Task<Void> {
        val db = FirebaseDatabase.getInstance()
        val dbref = db.getReference(Luchador::class.java.simpleName)
        return dbref.push().setValue(luc)
    }

    private fun listarLuchadores() {
        val db = FirebaseDatabase.getInstance()
        val dbref = db.getReference(Luchador::class.java.simpleName)
        val lisluc = ArrayList<Luchador>()
        val ada = ArrayAdapter(this, android.R.layout.simple_list_item_1, lisluc)
        lvDatos.adapter = ada
        lisluc.clear()
        ada.notifyDataSetChanged()
        dbref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val luc = snapshot.getValue(Luchador::class.java)
                luc?.let {
                    lisluc.add(it)
                    ada.notifyDataSetChanged()
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                ada.notifyDataSetChanged()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                listarLuchadores()
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
        lvDatos.setOnItemClickListener { _, _, position, _ ->
            ocultarTeclado()
            val luc = lisluc[position]
            txtid.setText(luc.id.toString())
            txtnom.setText(luc.nombre ?: "")
        }
        lvDatos.setOnItemLongClickListener { _, _, position, _ ->
            val luc = lisluc[position]
            val aux = luc.id.toString()
            val db2 = FirebaseDatabase.getInstance()
            val dbref2 = db2.getReference(Luchador::class.java.simpleName)
            dbref2.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var res = false
                    for (x in snapshot.children) {
                        val idVal = x.child("id").getValue()?.toString()
                        if (idVal != null && idVal.equals(aux, ignoreCase = true)) {
                            res = true
                            val a = AlertDialog.Builder(this@MainActivity)
                            a.setTitle("Pregunta")
                            a.setMessage("¿Está Seguro(a) De Querer Eliminar El Registro ($aux)?")
                            a.setCancelable(false)
                            a.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                            a.setPositiveButton("Aceptar") { _, _ ->
                                x.ref.removeValue()
                                listarLuchadores()
                                ocultarTeclado()
                                Toast.makeText(this@MainActivity, "Registro ($aux) Eliminado Correctamente!!", Toast.LENGTH_SHORT).show()
                                txtid.setText("")
                                txtnom.setText("")
                                txtid.requestFocus()
                            }
                            a.show()
                            break
                        }
                    }
                    if (!res) {
                        ocultarTeclado()
                        Toast.makeText(this@MainActivity, "Error. Id ($aux) No Encontrado. Imposible Eliminar!!", Toast.LENGTH_SHORT).show()
                        txtid.setText("")
                        txtnom.setText("")
                        txtid.requestFocus()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            true
        }
    }

    private fun botonBuscar() {
        btnbus.setOnClickListener {
            if (txtid.text.toString().trim().isEmpty()) {
                ocultarTeclado()
                Toast.makeText(this, "Indique El Id Para Buscar!!", Toast.LENGTH_SHORT).show()
                txtid.setText("")
                txtid.requestFocus()
            } else {
                val id = txtid.text.toString().toInt()
                val aux = id.toString()
                val db = FirebaseDatabase.getInstance()
                val dbref = db.getReference(Luchador::class.java.simpleName)
                dbref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var res = false
                        for (x in snapshot.children) {
                            val idVal = x.child("id").getValue()?.toString()
                            if (idVal != null && idVal.equals(aux, ignoreCase = true)) {
                                res = true
                                txtnom.setText(x.child("nombre").getValue()?.toString() ?: "")
                                break
                            }
                        }
                        if (!res) {
                            ocultarTeclado()
                            Toast.makeText(this@MainActivity, "Error. Id ($aux) No Encontrado!!", Toast.LENGTH_SHORT).show()
                            txtid.setText("")
                            txtnom.setText("")
                            txtid.requestFocus()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun botonModificar() {
        btnmod.setOnClickListener {
            if (txtid.text.toString().trim().isEmpty() || txtnom.text.toString().trim().isEmpty()) {
                ocultarTeclado()
                Toast.makeText(this, "Complete Los Campos Para Continuar!!!!", Toast.LENGTH_SHORT).show()
                txtid.setText("")
                txtnom.setText("")
                txtid.requestFocus()
            } else {
                val id = txtid.text.toString().toInt()
                val nom = txtnom.text.toString()
                val aux = id.toString()

                val db = FirebaseDatabase.getInstance()
                val dbref = db.getReference(Luchador::class.java.simpleName)

                dbref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var res = false
                        var res2 = false
                        var targetSnapshot: DataSnapshot? = null
                        for (x in snapshot.children) {
                            val idVal = x.child("id").getValue()?.toString()
                            val nomVal = x.child("nombre").getValue()?.toString()
                            if (idVal != null && idVal.equals(aux, ignoreCase = true)) {
                                res = true
                                targetSnapshot = x
                            }
                            if (nomVal != null && nomVal.equals(nom, ignoreCase = true)) {
                                res2 = true
                            }
                            if (res && !res2 && targetSnapshot != null) {
                                val a = AlertDialog.Builder(this@MainActivity)
                                a.setTitle("Pregunta")
                                a.setMessage("¿Está Seguro(a) De Querer Modificar El Nombre Del Registro ($aux)?")
                                a.setCancelable(false)
                                a.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                a.setPositiveButton("Aceptar") { _, _ ->
                                    targetSnapshot.ref.child("nombre").setValue(nom)
                                    listarLuchadores()
                                    ocultarTeclado()
                                    Toast.makeText(this@MainActivity, "Dato Modificado Correctamente!!", Toast.LENGTH_SHORT).show()
                                    txtid.setText("")
                                    txtnom.setText("")
                                    txtid.requestFocus()
                                }
                                a.show()
                                break
                            }
                        }
                        if (!res) {
                            ocultarTeclado()
                            Toast.makeText(this@MainActivity, "Error. Id ($aux) No Encontrado. Imposible Modificar Nombre!!", Toast.LENGTH_SHORT).show()
                            txtid.setText("")
                            txtnom.setText("")
                            txtid.requestFocus()
                        } else if (res2) {
                            ocultarTeclado()
                            Toast.makeText(this@MainActivity, "Error. El Nombre ($nom) Ya Existe. Imposible Modificar Nombre En Uso!!", Toast.LENGTH_SHORT).show()
                            txtid.setText("")
                            txtnom.setText("")
                            txtid.requestFocus()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun botonEliminar() {
        btneli.setOnClickListener {
            if (txtid.text.toString().trim().isEmpty()) {
                ocultarTeclado()
                Toast.makeText(this, "Indique El Id Para Eliminar!!", Toast.LENGTH_SHORT).show()
                txtid.setText("")
                txtid.requestFocus()
            } else {
                val id = txtid.text.toString().toInt()
                val aux = id.toString()

                val db = FirebaseDatabase.getInstance()
                val dbref = db.getReference(Luchador::class.java.simpleName)

                dbref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var res = false
                        for (x in snapshot.children) {
                            val idVal = x.child("id").getValue()?.toString()
                            if (idVal != null && idVal.equals(aux, ignoreCase = true)) {
                                res = true
                                val a = AlertDialog.Builder(this@MainActivity)
                                a.setTitle("Pregunta")
                                a.setMessage("¿Está Seguro(a) De Querer Eliminar El Registro ($aux)?")
                                a.setCancelable(false)
                                a.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                                a.setPositiveButton("Aceptar") { _, _ ->
                                    x.ref.removeValue()
                                    listarLuchadores()
                                    ocultarTeclado()
                                    Toast.makeText(this@MainActivity, "Registro ($aux) Eliminado Correctamente!!", Toast.LENGTH_SHORT).show()
                                    txtid.setText("")
                                    txtnom.setText("")
                                    txtid.requestFocus()
                                }
                                a.show()
                                break
                            }
                        }
                        if (!res) {
                            ocultarTeclado()
                            Toast.makeText(this@MainActivity, "Error. Id ($aux) No Encontrado. Imposible Eliminar!!", Toast.LENGTH_SHORT).show()
                            txtid.setText("")
                            txtnom.setText("")
                            txtid.requestFocus()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun ocultarTeclado() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
