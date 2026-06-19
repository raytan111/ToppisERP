// ============================================================================
// Edge Function: admin-usuarios
// ----------------------------------------------------------------------------
// Operaciones administrativas sobre cuentas de Auth que requieren service_role:
//   - action "eliminar":      borra el usuario de auth.users (y su perfil cae
//                             por ON DELETE CASCADE) -> borrado TOTAL.
//   - action "reset_password": cambia la contraseña de un usuario.
//
// Seguridad: valida el JWT del llamante y exige que su rol en la tabla
// "usuarios" sea ADMIN. Sin eso, responde 403.
//
// Deploy:
//   supabase functions deploy admin-usuarios
// (SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY ya están disponibles como env en
//  el entorno de Edge Functions de Supabase.)
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

    // 1) Identificar al llamante a partir de su JWT
    const authHeader = req.headers.get("Authorization") ?? "";
    const token = authHeader.replace("Bearer ", "");
    if (!token) {
      return json({ error: "Falta el token de autenticación." }, 401);
    }

    const asCaller = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${token}` } },
    });
    const { data: userData, error: userErr } = await asCaller.auth.getUser();
    if (userErr || !userData?.user) {
      return json({ error: "Sesión inválida." }, 401);
    }
    const callerId = userData.user.id;

    // 2) Cliente admin (service_role) para verificar rol y ejecutar acciones
    const admin = createClient(supabaseUrl, serviceKey);

    const { data: perfil, error: perfilErr } = await admin
      .from("usuarios")
      .select("rol")
      .eq("id", callerId)
      .single();
    if (perfilErr || !perfil) {
      return json({ error: "No se encontró el perfil del llamante." }, 403);
    }
    if (perfil.rol !== "ADMIN") {
      return json({ error: "Solo un ADMIN puede realizar esta operación." }, 403);
    }

    // 3) Procesar la acción
    const body = await req.json();
    const action = body?.action as string;
    const usuarioId = body?.usuario_id as string;

    if (!usuarioId) {
      return json({ error: "Falta usuario_id." }, 400);
    }
    if (usuarioId === callerId) {
      return json({ error: "No podés realizar esta acción sobre tu propia cuenta." }, 400);
    }

    if (action === "eliminar") {
      // Borra de auth.users; el perfil en "usuarios" cae por ON DELETE CASCADE.
      const { error } = await admin.auth.admin.deleteUser(usuarioId);
      if (error) return json({ error: error.message }, 400);
      return json({ ok: true });
    }

    if (action === "reset_password") {
      const nuevaPassword = body?.password as string;
      if (!nuevaPassword || nuevaPassword.length < 6) {
        return json({ error: "La contraseña debe tener al menos 6 caracteres." }, 400);
      }
      const { error } = await admin.auth.admin.updateUserById(usuarioId, {
        password: nuevaPassword,
      });
      if (error) return json({ error: error.message }, 400);
      return json({ ok: true });
    }

    return json({ error: `Acción no soportada: ${action}` }, 400);
  } catch (e) {
    return json({ error: String(e?.message ?? e) }, 500);
  }
});

function json(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
