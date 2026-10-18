const { test, describe } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const UserStorage = require('../src/userStorage');

describe('UserStorage & Role-Based Access Control (RBAC)', () => {
  const testDataDir = path.join(__dirname, 'temp_test_data');
  const appConfig = {
    authorized_emails: ['proyectoing2026@gmail.com', 'docente.saberpro@institucion.edu.co']
  };

  test('Determines superusuario role for authorized emails and estudiante for regular accounts', () => {
    const storage = new UserStorage(appConfig, testDataDir);
    assert.strictEqual(storage.determineRole('proyectoing2026@gmail.com'), 'superusuario');
    assert.strictEqual(storage.determineRole('docente.saberpro@institucion.edu.co'), 'superusuario');
    assert.strictEqual(storage.determineRole('keilawhp@gmail.com'), 'estudiante');
    assert.strictEqual(storage.determineRole('estudiante.random@gmail.com'), 'estudiante');
  });

  test('findOrCreateGoogleUser creates user and session token correctly', () => {
    const storage = new UserStorage(appConfig, testDataDir);
    const { user, token } = storage.findOrCreateGoogleUser({
      email: 'keilawhp@gmail.com',
      name: 'Keila'
    });

    assert.strictEqual(user.email, 'keilawhp@gmail.com');
    assert.strictEqual(user.role, 'estudiante');
    assert.ok(token);

    const session = storage.getSession(token);
    assert.ok(session);
    assert.strictEqual(session.email, 'keilawhp@gmail.com');
    assert.strictEqual(session.role, 'estudiante');
  });

  test('Superuser privileges correctly verified', () => {
    const storage = new UserStorage(appConfig, testDataDir);
    assert.strictEqual(storage.isSuperuserOrTeacher('proyectoing2026@gmail.com'), true);
    assert.strictEqual(storage.isSuperuserOrTeacher('keilawhp@gmail.com'), false);
    assert.strictEqual(storage.isStudent('keilawhp@gmail.com'), true);
    assert.strictEqual(storage.isStudent('proyectoing2026@gmail.com'), false);

    // Cleanup temp files
    try {
      fs.rmSync(testDataDir, { recursive: true, force: true });
    } catch (_) {}
  });
});
