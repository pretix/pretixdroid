package eu.pretix.pretixdroid.net.server;

import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VerifyKeyServlet extends HttpServlet {
    private String signature;

    public void init(ServletConfig servletConfig) throws ServletException {
        signature = servletConfig.getInitParameter("signature");
        super.init(servletConfig);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setStatus(HttpStatus.OK_200);
        resp.getWriter().println(signature);
    }
}
