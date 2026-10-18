/**
 * Acierta Pro — Persistencia de Usuarios, Sesiones y Control de Roles (RBAC)
 * Base de datos persistente en JSON con soporte para Google Sign-In y cuentas locales.
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class UserStorage {
  constructor(appConfig, dataDir) {
    this.appConfig = appConfig;
    this.dataDir = dataDir || path.join(__dirname, '..', 'data');
    this.usersFile = path.join(this.dataDir, 'users.json');
    this.sessionsFile = path.join(this.dataDir, 'sessions.json');
    this.initStorage();
  }

  initStorage() {
    if (!fs.existsSync(this.dataDir)) {
      fs.mkdirSync(this.dataDir, { recursive: true });
    }

    if (!fs.existsSync(this.usersFile)) {
      const initialUsers = [
        {
          id: 'usr_super_01',
          email: 'proyectoing2026@gmail.com',
          name: 'Superusuario AciertaPro',
          role: 'superusuario',
          provider: 'google',
          picture: 'https://lh3.googleusercontent.com/a/default-user',
          academic_program: 'Ingeniería de Sistemas',
          campus: 'Sede Maicao',
          created_at: new Date().toISOString()
        },
        {
          id: 'usr_teacher_01',
          email: 'docente.saberpro@institucion.edu.co',
          name: 'Docente Evaluador ICFES',
          role: 'docente',
          provider: 'google',
          picture: 'https://lh3.googleusercontent.com/a/default-user',
          academic_program: 'Ciencias de la Educación',
          campus: 'Sede Principal',
          created_at: new Date().toISOString()
        },
        {
          id: 'usr_student_01',
          email: 'keilawhp@gmail.com',
          name: 'Keila - Estudiante',
          role: 'estudiante',
          provider: 'google',
          picture: 'https://lh3.googleusercontent.com/a/default-user',
          academic_program: 'Ingeniería de Sistemas',
          campus: 'Sede Maicao',
          created_at: new Date().toISOString()
        }
      ];
      fs.writeFileSync(this.usersFile, JSON.stringify(initialUsers, null, 2), 'utf-8');
    }

    if (!fs.existsSync(this.sessionsFile)) {
      fs.writeFileSync(this.sessionsFile, JSON.stringify({}, null, 2), 'utf-8');
    }
  }

  _readUsers() {
    try {
      return JSON.parse(fs.readFileSync(this.usersFile, 'utf-8'));
    } catch (_) {
      return [];
    }
  }

  _writeUsers(users) {
    fs.writeFileSync(this.usersFile, JSON.stringify(users, null, 2), 'utf-8');
  }

  _readSessions() {
    try {
      return JSON.parse(fs.readFileSync(this.sessionsFile, 'utf-8'));
    } catch (_) {
      return {};
    }
  }

  _writeSessions(sessions) {
    fs.writeFileSync(this.sessionsFile, JSON.stringify(sessions, null, 2), 'utf-8');
  }

  getAuthorizedEmails() {
    return (this.appConfig.authorized_emails || []).map(e => e.toLowerCase());
  }

  determineRole(email) {
    const cleanEmail = (email || '').trim().toLowerCase();
    const authorized = this.getAuthorizedEmails();
    if (authorized.includes(cleanEmail)) {
      return 'superusuario';
    }
    return 'estudiante';
  }

  getUserByEmail(email) {
    if (!email) return null;
    const cleanEmail = email.trim().toLowerCase();
    const users = this._readUsers();
    return users.find(u => u.email.toLowerCase() === cleanEmail) || null;
  }

  findOrCreateGoogleUser({ email, name, picture, google_id, academic_program, campus }) {
    if (!email) throw new Error('El correo electrónico es requerido para autenticación');
    const cleanEmail = email.trim().toLowerCase();
    const users = this._readUsers();
    let user = users.find(u => u.email.toLowerCase() === cleanEmail);

    if (user) {
      // Si el correo está en la lista de autorizados, garantizar rol superusuario
      const expectedRole = this.getAuthorizedEmails().includes(cleanEmail) ? 'superusuario' : user.role;
      user.name = name || user.name;
      user.picture = picture || user.picture;
      user.google_id = google_id || user.google_id;
      user.role = expectedRole;
      user.last_login = new Date().toISOString();
      this._writeUsers(users);
    } else {
      const assignedRole = this.determineRole(cleanEmail);
      user = {
        id: 'usr_' + crypto.randomBytes(6).toString('hex'),
        email: cleanEmail,
        name: name || cleanEmail.split('@')[0],
        role: assignedRole,
        provider: 'google',
        picture: picture || 'https://lh3.googleusercontent.com/a/default-user',
        google_id: google_id || null,
        academic_program: academic_program || 'Ingeniería de Sistemas',
        campus: campus || 'Sede Maicao',
        created_at: new Date().toISOString(),
        last_login: new Date().toISOString()
      };
      users.push(user);
      this._writeUsers(users);
    }

    // Crear sesión persistente
    const token = crypto.randomBytes(24).toString('hex');
    const sessions = this._readSessions();
    sessions[token] = {
      user_id: user.id,
      email: user.email,
      role: user.role,
      name: user.name,
      created_at: Date.now()
    };
    this._writeSessions(sessions);

    return { user, token };
  }

  loginLocalOrDemo(email) {
    if (!email) throw new Error('El correo electrónico es requerido');
    return this.findOrCreateGoogleUser({
      email,
      name: email.split('@')[0],
      picture: 'https://lh3.googleusercontent.com/a/default-user'
    });
  }

  getSession(token) {
    if (!token) return null;
    const sessions = this._readSessions();
    return sessions[token] || null;
  }

  removeSession(token) {
    if (!token) return;
    const sessions = this._readSessions();
    if (sessions[token]) {
      delete sessions[token];
      this._writeSessions(sessions);
    }
  }

  isSuperuserOrTeacher(email) {
    if (!email) return false;
    const cleanEmail = email.trim().toLowerCase();
    if (this.getAuthorizedEmails().includes(cleanEmail)) return true;
    const user = this.getUserByEmail(cleanEmail);
    return user ? (user.role === 'superusuario' || user.role === 'docente') : false;
  }

  isStudent(email) {
    return !this.isSuperuserOrTeacher(email);
  }
}

module.exports = UserStorage;
