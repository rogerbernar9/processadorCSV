package org.csvprocessor.service;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class AutenticadorLDAP {

    public static boolean autenticar(String usuario, String senha) {
        String ldapURL = "ldap://172.17.1.7:389";
        String baseDN = "dc=empresa,dc=local";
        String userDN = "pmsg\\" + usuario;
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDN);
        env.put(Context.SECURITY_CREDENTIALS, senha);
        try {
            DirContext ctx = new InitialDirContext(env);
            ctx.close();
            return true;
        } catch (AuthenticationException e) {
            return false;
        } catch (NamingException e) {
            System.err.println("Erro LDAP: " + e.getMessage());
            return false;
        }
    }
}
