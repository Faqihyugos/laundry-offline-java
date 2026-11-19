package com.example.ui.views.login;

import com.example.security.AuthenticatedUser;
import com.example.service.SettingService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Laundry POS")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();
    private final AuthenticatedUser authenticatedUser;
    private final SettingService settingService;

    public LoginView(AuthenticatedUser authenticatedUser, SettingService settingService) {
        this.authenticatedUser = authenticatedUser;
        this.settingService = settingService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle().set("background-color", "#FAF7FB");

        Div card = new Div();
        card.getStyle().set("background", "white")
                .set("padding", "2rem")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(123, 45, 142, 0.12)")
                .set("max-width", "420px")
                .set("width", "100%")
                .set("text-align", "center");

        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);

        Div logoBox = new Div();
        logoBox.getStyle().set("width", "48px")
                .set("height", "48px")
                .set("border-radius", "12px")
                .set("background", "#6D28D9")
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("margin-bottom", "12px");
        Icon washerIcon = VaadinIcon.TASKS.create();
        washerIcon.setSize("24px");
        washerIcon.setColor("#FFFFFF");
        logoBox.add(washerIcon);

        H2 brandTitle = new H2(laundryName);
        brandTitle.getStyle().set("color", "#0F172A")
                .set("margin", "0 0 0.5rem 0")
                .set("font-size", "1.5rem")
                .set("font-weight", "800");

        Paragraph subtitle = new Paragraph("Sistem Manajemen Laundry & Point-of-Sale");
        subtitle.getStyle().set("color", "#64748B")
                .set("margin", "0 0 1.5rem 0")
                .set("font-size", "0.9rem");

        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form form = i18n.getForm();
        form.setTitle("");
        form.setUsername("Username");
        form.setPassword("Password");
        form.setSubmit("Masuk");
        i18n.setForm(form);
        LoginI18n.ErrorMessage errorMsg = i18n.getErrorMessage();
        errorMsg.setTitle("Login Gagal");
        errorMsg.setMessage("Username atau password salah, atau akun telah dinonaktifkan.");
        i18n.setErrorMessage(errorMsg);

        loginForm.setI18n(i18n);
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        Div demoBox = new Div();
        demoBox.getStyle().set("background-color", "#F1F5F9")
                .set("border", "1px dashed #CBD5E1")
                .set("border-radius", "8px")
                .set("padding", "0.75rem")
                .set("margin-top", "1rem")
                .set("font-size", "0.8rem")
                .set("text-align", "left")
                .set("color", "#334155");

        demoBox.add(new Span("Akun Demo Default:"));
        Div d1 = new Div();
        d1.setText("• Owner: admin / admin123");
        Div d2 = new Div();
        d2.setText("• Kasir: kasir / kasir123");
        demoBox.add(d1, d2);

        card.add(logoBox, brandTitle, subtitle, loginForm, demoBox);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticatedUser.get().isPresent()) {
            event.forwardTo("");
            return;
        }

        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
