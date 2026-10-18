package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SaberProDatabase
import com.example.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserAuthRoleTest {

    private lateinit var db: SaberProDatabase
    private lateinit var repository: UserDataRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SaberProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UserDataRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoleDetermination_authorizedSuperuserVsStudent() {
        val superRole = repository.determineRole("proyectoing2026@gmail.com")
        val teacherRole = repository.determineRole("docente.saberpro@institucion.edu.co")
        val studentRole = repository.determineRole("keilawhp@gmail.com")
        val unknownRole = repository.determineRole("estudiante.nuevo@uniguajira.edu.co")

        assertEquals(UserRole.SUPERUSER, superRole)
        assertEquals(UserRole.SUPERUSER, teacherRole)
        assertEquals(UserRole.STUDENT, studentRole)
        assertEquals(UserRole.STUDENT, unknownRole)
    }

    @Test
    fun testGoogleLoginAndSessionSeparation_noRoleLeak() = runBlocking {
        // 1. Iniciar sesión como superusuario
        val superResult = repository.loginWithGoogle("proyectoing2026@gmail.com", "Super Administrador")
        val superProfile = superResult.getOrThrow()
        assertEquals(UserRole.SUPERUSER, superProfile.role)
        assertEquals("proyectoing2026@gmail.com", superProfile.email)

        // 2. Cerrar sesión
        repository.logout()

        // 3. Iniciar sesión como estudiante
        val studentResult = repository.loginWithGoogle("keilawhp@gmail.com", "Keila Estudiante")
        val studentProfile = studentResult.getOrThrow()

        // 4. Verificar que el perfil de estudiante NO tiene privilegios de superusuario
        assertEquals(UserRole.STUDENT, studentProfile.role)
        assertEquals("keilawhp@gmail.com", studentProfile.email)
        assertNotEquals(UserRole.SUPERUSER, studentProfile.role)
    }
}
