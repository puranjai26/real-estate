const STORAGE_TOKEN_KEY = "crmToken";
const STORAGE_USER_KEY = "crmUser";

const state = {
    leads: { page: 0, size: 8, items: [] },
    properties: { page: 0, size: 8, items: [] },
    clients: { page: 0, size: 8, items: [] },
    agents: { page: 0, size: 8, items: [] },
    bookings: { page: 0, size: 8, items: [] },
    agentOptions: [],
    propertyOptions: [],
    clientOptions: []
};

document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    ensureSharedUi();
    if (page !== "login") {
        requireAuth();
        hydrateShell(page);
    }

    switch (page) {
        case "login":
            initLoginPage();
            break;
        case "dashboard":
            initDashboardPage();
            break;
        case "leads":
            initLeadsPage();
            break;
        case "properties":
            initPropertiesPage();
            break;
        case "clients":
            initClientsPage();
            break;
        case "agents":
            initAgentsPage();
            break;
        case "bookings":
            initBookingsPage();
            break;
        default:
            break;
    }
});

function requireAuth() {
    if (!getToken()) {
        window.location.href = "/";
    }
}

function hydrateShell(page) {
    const user = getUser();
    document.querySelectorAll("[data-nav]").forEach((link) => {
        link.classList.toggle("active", link.dataset.nav === page);
    });

    const currentUserLabel = document.getElementById("currentUserLabel");
    if (currentUserLabel && user) {
        currentUserLabel.textContent = `${user.name} · ${formatRole(user.role)}`;
    }

    if (user?.role !== "ADMIN") {
        document.querySelectorAll(".admin-only").forEach((node) => node.classList.add("hidden"));
    }

    const logoutButton = document.getElementById("logoutButton");
    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }

    const logoutButtonMobile = document.getElementById("logoutButtonMobile");
    if (logoutButtonMobile) {
        logoutButtonMobile.addEventListener("click", logout);
    }
}

function initLoginPage() {
    if (getToken()) {
        window.location.href = "/dashboard.html";
        return;
    }

    document.getElementById("loginForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        try {
            const response = await apiFetch("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: document.getElementById("loginEmail").value.trim(),
                    password: document.getElementById("loginPassword").value
                })
            }, false);
            setAuth(response);
            showToast("Login successful");
            window.location.href = "/dashboard.html";
        } catch (error) {
            showToast(error.message || "Login failed", true);
        }
    });
}

async function initDashboardPage() {
    bindExportButtons();
    try {
        const [summary, properties, bookings] = await Promise.all([
            apiFetch("/api/dashboard/summary"),
            apiFetch("/api/properties?page=0&size=5"),
            apiFetch("/api/bookings?page=0&size=5")
        ]);
        renderMetrics(summary);
        renderPortfolioHighlight(summary);
        renderPaymentSnapshot(summary);
        renderDistributionList("propertyTypeMix", summary.propertyTypeMix, "No type mix available yet.");
        renderDistributionList("cityMix", summary.cityMix, "No city demand data yet.");
        renderDistributionList("leadStageMix", summary.leadStageMix, "Lead stage data will appear here after onboarding enquiries.");
        renderDistributionList("leadSourceMix", summary.leadSourceMix, "Lead source data will appear here after onboarding enquiries.");
        renderBrokerLeaderboard(summary.topBrokers);
        renderUpcomingBookings(summary.upcomingBookings);
        renderLeadFollowUps(summary.upcomingLeadFollowUps);
        renderRecentProperties(properties.content);
        renderRecentBookings(bookings.content);
    } catch (error) {
        showToast(error.message || "Failed to load dashboard", true);
    }
}

async function initLeadsPage() {
    bindExportButtons();
    await loadAgentOptions();
    bindLeadForm();
    bindLeadFilters();
    await loadLeads();
}

async function initPropertiesPage() {
    await loadAgentOptions();
    bindPropertyForm();
    bindPropertyFilters();
    await loadProperties();
}

async function initClientsPage() {
    bindExportButtons();
    bindClientForm();
    await loadClients();
}

async function initAgentsPage() {
    if (getUser()?.role !== "ADMIN") {
        showToast("Only admins can manage brokers", true);
        window.location.href = "/dashboard.html";
        return;
    }
    bindAgentForm();
    await loadAgents();
}

async function initBookingsPage() {
    await Promise.all([loadAgentOptions(), loadPropertyOptions(), loadClientOptions()]);
    bindBookingForm();
    bindBookingFilters();
    await loadBookings();
}

function bindExportButtons() {
    const leadsButton = document.getElementById("exportLeadsButton");
    const propertiesButton = document.getElementById("exportPropertiesButton");
    const clientsButton = document.getElementById("exportClientsButton");
    const bookingsButton = document.getElementById("exportBookingsButton");

    if (leadsButton) {
        leadsButton.addEventListener("click", () => downloadFile("/api/export/leads.csv", "leads.csv"));
    }
    if (propertiesButton) {
        propertiesButton.addEventListener("click", () => downloadFile("/api/export/properties.csv", "properties.csv"));
    }
    if (clientsButton) {
        clientsButton.addEventListener("click", () => downloadFile("/api/export/clients.csv", "clients.csv"));
    }
    if (bookingsButton) {
        bookingsButton.addEventListener("click", () => downloadFile("/api/export/bookings.xlsx", "bookings.xlsx"));
    }
}

function bindLeadForm() {
    document.getElementById("leadForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const leadId = document.getElementById("leadId").value;
        const payload = {
            name: document.getElementById("leadName").value.trim(),
            email: document.getElementById("leadEmail").value.trim(),
            phone: document.getElementById("leadPhone").value.trim(),
            preferredLocation: document.getElementById("leadPreferredLocation").value.trim(),
            budgetMin: numberOrNull(document.getElementById("leadBudgetMin").value),
            budgetMax: numberOrNull(document.getElementById("leadBudgetMax").value),
            interestType: document.getElementById("leadInterestType").value,
            source: document.getElementById("leadSource").value,
            followUpDate: blankToNull(document.getElementById("leadFollowUpDate").value),
            stage: document.getElementById("leadStage").value,
            notes: blankToNull(document.getElementById("leadNotes").value),
            agentId: valueOrNull(document.getElementById("leadAgentId").value)
        };

        try {
            await apiFetch(leadId ? `/api/leads/${leadId}` : "/api/leads", {
                method: leadId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showToast(leadId ? "Lead updated" : "Lead created");
            resetLeadForm();
            await loadLeads();
        } catch (error) {
            showToast(error.message || "Failed to save lead", true);
        }
    });

    document.getElementById("cancelLeadEdit").addEventListener("click", resetLeadForm);
    document.getElementById("leadTableBody").addEventListener("click", handleLeadTableActions);
}

function bindLeadFilters() {
    document.getElementById("leadFilterForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        state.leads.page = 0;
        await loadLeads();
    });
    document.getElementById("resetLeadFilters").addEventListener("click", async () => {
        document.getElementById("leadFilterForm").reset();
        state.leads.page = 0;
        await loadLeads();
    });
}

function bindPropertyForm() {
    document.getElementById("propertyForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const propertyId = document.getElementById("propertyId").value;
        const city = document.getElementById("propertyCity").value.trim();
        const locality = document.getElementById("propertyLocality").value.trim();
        const payload = {
            title: document.getElementById("propertyTitle").value.trim(),
            location: `${locality}, ${city}`,
            city,
            locality,
            propertyType: document.getElementById("propertyType").value,
            configuration: document.getElementById("propertyConfiguration").value.trim(),
            areaSqFt: Number(document.getElementById("propertyAreaSqFt").value),
            price: Number(document.getElementById("propertyPrice").value),
            status: document.getElementById("propertyStatus").value,
            featured: document.getElementById("propertyFeatured").checked,
            agentId: valueOrNull(document.getElementById("propertyAgentId").value)
        };

        try {
            await apiFetch(propertyId ? `/api/properties/${propertyId}` : "/api/properties", {
                method: propertyId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showToast(propertyId ? "Property updated" : "Property created");
            resetPropertyForm();
            await Promise.all([loadProperties(), loadPropertyOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to save property", true);
        }
    });

    document.getElementById("cancelPropertyEdit").addEventListener("click", resetPropertyForm);
    document.getElementById("propertyTableBody").addEventListener("click", handlePropertyTableActions);
}

function bindPropertyFilters() {
    document.getElementById("propertyFilterForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        state.properties.page = 0;
        await loadProperties();
    });
    document.getElementById("resetPropertyFilters").addEventListener("click", async () => {
        document.getElementById("propertyFilterForm").reset();
        state.properties.page = 0;
        await loadProperties();
    });
}

function bindClientForm() {
    document.getElementById("clientForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const clientId = document.getElementById("clientId").value;
        const payload = {
            name: document.getElementById("clientName").value.trim(),
            email: document.getElementById("clientEmail").value.trim(),
            phone: document.getElementById("clientPhone").value.trim(),
            preferredLocation: blankToNull(document.getElementById("clientPreferredLocation").value)
        };

        try {
            await apiFetch(clientId ? `/api/clients/${clientId}` : "/api/clients", {
                method: clientId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showToast(clientId ? "Client updated" : "Client created");
            resetClientForm();
            await Promise.all([loadClients(), loadClientOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to save client", true);
        }
    });

    document.getElementById("cancelClientEdit").addEventListener("click", resetClientForm);
    document.getElementById("clientTableBody").addEventListener("click", handleClientTableActions);
}

function bindBookingFilters() {
    document.getElementById("bookingFilterForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        state.bookings.page = 0;
        await loadBookings();
    });
    document.getElementById("resetBookingFilters").addEventListener("click", async () => {
        document.getElementById("bookingFilterForm").reset();
        state.bookings.page = 0;
        await loadBookings();
    });
}

function bindAgentForm() {
    document.getElementById("agentForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const agentId = document.getElementById("agentId").value;
        const payload = {
            name: document.getElementById("agentName").value.trim(),
            email: document.getElementById("agentEmail").value.trim(),
            password: blankToNull(document.getElementById("agentPassword").value)
        };

        try {
            await apiFetch(agentId ? `/api/agents/${agentId}` : "/api/agents", {
                method: agentId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showToast(agentId ? "Broker updated" : "Broker created");
            resetAgentForm();
            await Promise.all([loadAgents(), loadAgentOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to save broker", true);
        }
    });

    document.getElementById("cancelAgentEdit").addEventListener("click", resetAgentForm);
    document.getElementById("agentTableBody").addEventListener("click", handleAgentTableActions);
}

function bindBookingForm() {
    document.getElementById("bookingForm").addEventListener("submit", async (event) => {
        event.preventDefault();
        const bookingId = document.getElementById("bookingId").value;
        const payload = {
            propertyId: Number(document.getElementById("bookingPropertyId").value),
            clientId: Number(document.getElementById("bookingClientId").value),
            agentId: Number(document.getElementById("bookingAgentId").value),
            bookingDate: document.getElementById("bookingDate").value,
            status: document.getElementById("bookingStatus").value,
            bookingAmount: Number(document.getElementById("bookingAmount").value),
            amountPaid: numberOrNull(document.getElementById("bookingAmountPaid").value) ?? 0,
            paymentStatus: document.getElementById("bookingPaymentStatus").value,
            paymentDate: blankToNull(document.getElementById("bookingPaymentDate").value),
            paymentReference: blankToNull(document.getElementById("bookingPaymentReference").value)
        };

        try {
            await apiFetch(bookingId ? `/api/bookings/${bookingId}` : "/api/bookings", {
                method: bookingId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showToast(bookingId ? "Booking updated" : "Booking created");
            resetBookingForm();
            await Promise.all([loadBookings(), loadPropertyOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to save booking", true);
        }
    });

    document.getElementById("cancelBookingEdit").addEventListener("click", resetBookingForm);
    document.getElementById("bookingTableBody").addEventListener("click", handleBookingTableActions);
}

async function loadProperties() {
    const params = new URLSearchParams({
        page: String(state.properties.page),
        size: String(state.properties.size)
    });

    addIfPresent(params, "location", document.getElementById("filterLocation")?.value.trim());
    addIfPresent(params, "city", document.getElementById("filterCity")?.value.trim());
    addIfPresent(params, "locality", document.getElementById("filterLocality")?.value.trim());
    addIfPresent(params, "propertyType", document.getElementById("filterPropertyType")?.value);
    addIfPresent(params, "configuration", document.getElementById("filterConfiguration")?.value.trim());
    addIfPresent(params, "minPrice", document.getElementById("filterMinPrice")?.value);
    addIfPresent(params, "maxPrice", document.getElementById("filterMaxPrice")?.value);
    addIfPresent(params, "status", document.getElementById("filterStatus")?.value);
    addIfPresent(params, "featured", document.getElementById("filterFeatured")?.value);

    const response = await apiFetch(`/api/properties?${params.toString()}`);
    state.properties.items = response.content;
    renderPropertyTable(response.content);
    renderPagination("propertyPagination", response, async (page) => {
        state.properties.page = page;
        await loadProperties();
    });
}

async function loadLeads() {
    const params = new URLSearchParams({
        page: String(state.leads.page),
        size: String(state.leads.size)
    });
    addIfPresent(params, "stage", document.getElementById("filterLeadStage")?.value);
    addIfPresent(params, "source", document.getElementById("filterLeadSource")?.value);
    addIfPresent(params, "agentId", document.getElementById("filterLeadAgentId")?.value);
    addIfPresent(params, "followUpDate", document.getElementById("filterLeadFollowUpDate")?.value);

    const response = await apiFetch(`/api/leads?${params.toString()}`);
    state.leads.items = response.content;
    renderLeadTable(response.content);
    renderLeadInsights(response.content);
    renderPagination("leadPagination", response, async (page) => {
        state.leads.page = page;
        await loadLeads();
    });
}

async function loadClients() {
    const response = await apiFetch(`/api/clients?page=${state.clients.page}&size=${state.clients.size}`);
    state.clients.items = response.content;
    renderClientTable(response.content);
    renderClientInsights(response.content);
    renderPagination("clientPagination", response, async (page) => {
        state.clients.page = page;
        await loadClients();
    });
}

async function loadAgents() {
    const response = await apiFetch(`/api/agents?page=${state.agents.page}&size=${state.agents.size}`);
    state.agents.items = response.content;
    renderAgentTable(response.content);
    renderAgentInsights(response.content);
    renderPagination("agentPagination", response, async (page) => {
        state.agents.page = page;
        await loadAgents();
    });
}

async function loadBookings() {
    const params = new URLSearchParams({
        page: String(state.bookings.page),
        size: String(state.bookings.size)
    });
    addIfPresent(params, "status", document.getElementById("filterBookingStatus")?.value);
    addIfPresent(params, "agentId", document.getElementById("filterBookingAgentId")?.value);
    addIfPresent(params, "fromDate", document.getElementById("filterBookingFromDate")?.value);
    addIfPresent(params, "toDate", document.getElementById("filterBookingToDate")?.value);
    const response = await apiFetch(`/api/bookings?${params.toString()}`);
    state.bookings.items = response.content;
    renderBookingTable(response.content);
    renderBookingInsights(response.content);
    renderPagination("bookingPagination", response, async (page) => {
        state.bookings.page = page;
        await loadBookings();
    });
}

async function loadAgentOptions() {
    const response = await apiFetch("/api/agents?page=0&size=100");
    state.agentOptions = response.content;
    fillOptions("leadAgentId", state.agentOptions, "Select broker", true);
    fillOptions("filterLeadAgentId", state.agentOptions, "All brokers", true);
    fillOptions("propertyAgentId", state.agentOptions, "Select broker", true);
    fillOptions("bookingAgentId", state.agentOptions, "Select broker");
    fillOptions("filterBookingAgentId", state.agentOptions, "All brokers", true);
}

async function loadPropertyOptions() {
    const response = await apiFetch("/api/properties?page=0&size=100");
    state.propertyOptions = response.content;
    fillOptions("bookingPropertyId", state.propertyOptions, "Select property", false, (item) =>
        `${item.title} · ${formatPropertyType(item.propertyType)} · ${item.locality}, ${item.city}`);
}

async function loadClientOptions() {
    const response = await apiFetch("/api/clients?page=0&size=100");
    state.clientOptions = response.content;
    fillOptions("bookingClientId", state.clientOptions, "Select client", false, (item) => item.name);
}

function renderMetrics(summary) {
    const metrics = [
        { label: "Leads", value: summary.totalLeads, helper: "Top funnel enquiries" },
        { label: "Total Properties", value: summary.totalProperties, helper: "Full active inventory" },
        { label: "Available", value: summary.availableProperties, helper: "Ready to pitch" },
        { label: "Sold", value: summary.soldProperties, helper: "Closed inventory" },
        { label: "Clients", value: summary.totalClients, helper: "Buyer and investor base" },
        { label: "Bookings", value: summary.totalBookings, helper: "All recorded visits and tokens" },
        { label: "Pending", value: summary.pendingBookings, helper: "Needs broker follow-up" },
        { label: "Confirmed", value: summary.confirmedBookings, helper: `${summary.bookingConversionRate}% conversion rate` },
        { label: "Inventory Value", value: formatCurrency(summary.totalPortfolioValue), helper: "Combined rupee value" }
    ];
    document.getElementById("metricsGrid").innerHTML = metrics.map((metric) => `
        <article class="metric-panel">
            <p class="eyebrow">${metric.label}</p>
            <strong>${metric.value}</strong>
            <span class="muted">${metric.helper}</span>
        </article>
    `).join("");
}

function renderPortfolioHighlight(summary) {
    const container = document.getElementById("portfolioHighlight");
    if (!container) {
        return;
    }
    container.innerHTML = `
        <div class="highlight-row">
            <span class="highlight-label">Available inventory</span>
            <strong>${formatCurrency(summary.availableInventoryValue)}</strong>
        </div>
        <div class="highlight-row">
            <span class="highlight-label">Sold inventory</span>
            <strong>${formatCurrency(summary.soldInventoryValue)}</strong>
        </div>
        <div class="highlight-row">
            <span class="highlight-label">Average ticket size</span>
            <strong>${formatCurrency(summary.averagePropertyPrice)}</strong>
        </div>
        <div class="conversion-card">
            <p class="eyebrow">Booking Conversion</p>
            <strong>${summary.bookingConversionRate}%</strong>
            <span class="muted">Confirmed bookings out of total booking records</span>
        </div>
    `;
}

function renderPaymentSnapshot(summary) {
    const container = document.getElementById("paymentSnapshot");
    if (!container) {
        return;
    }
    container.innerHTML = `
        <div class="highlight-row">
            <span class="highlight-label">Booking value</span>
            <strong>${formatCurrency(summary.totalBookingValue)}</strong>
        </div>
        <div class="highlight-row">
            <span class="highlight-label">Collected</span>
            <strong>${formatCurrency(summary.collectedPayments)}</strong>
        </div>
        <div class="highlight-row">
            <span class="highlight-label">Outstanding</span>
            <strong>${formatCurrency(summary.outstandingPayments)}</strong>
        </div>
    `;
}

function renderDistributionList(containerId, items, emptyMessage) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    if (!items?.length) {
        container.innerHTML = `<p class="empty-state">${emptyMessage}</p>`;
        return;
    }
    const max = Math.max(...items.map((item) => item.value), 1);
    container.innerHTML = items.map((item) => `
        <div class="distribution-item">
            <div class="distribution-copy">
                <span class="table-title">${item.label}</span>
                <span class="table-subtext">${item.value} records</span>
            </div>
            <div class="distribution-track">
                <span class="distribution-bar" style="width:${Math.max((item.value / max) * 100, 12)}%"></span>
            </div>
        </div>
    `).join("");
}

function renderBrokerLeaderboard(brokers) {
    const container = document.getElementById("brokerLeaderboard");
    if (!container) {
        return;
    }
    if (!brokers?.length) {
        container.innerHTML = `<p class="empty-state">Broker performance will appear here once deals start moving.</p>`;
        return;
    }
    container.innerHTML = brokers.map((broker, index) => `
        <div class="leaderboard-item">
            <div class="leaderboard-rank">0${index + 1}</div>
            <div>
                <span class="table-title">${broker.name}</span>
                <span class="table-subtext">${broker.propertyCount} listings · ${broker.bookingCount} bookings</span>
            </div>
            <strong>${broker.confirmedDeals} closed</strong>
        </div>
    `).join("");
}

function renderUpcomingBookings(bookings) {
    const container = document.getElementById("upcomingBookings");
    if (!container) {
        return;
    }
    if (!bookings?.length) {
        container.innerHTML = `<p class="empty-state">No upcoming bookings scheduled.</p>`;
        return;
    }
    container.innerHTML = bookings.map((booking) => `
        <div class="upcoming-item">
            <div>
                <span class="table-title">${booking.propertyTitle}</span>
                <span class="table-subtext">${booking.clientName} · ${booking.location}</span>
            </div>
            <div class="upcoming-meta">
                <strong>${formatDate(booking.bookingDate)}</strong>
                ${renderStatus(booking.status)}
            </div>
        </div>
    `).join("");
}

function renderLeadFollowUps(leads) {
    const container = document.getElementById("leadFollowUps");
    if (!container) {
        return;
    }
    if (!leads?.length) {
        container.innerHTML = `<p class="empty-state">No scheduled lead follow-ups right now.</p>`;
        return;
    }
    container.innerHTML = leads.map((lead) => `
        <div class="upcoming-item">
            <div>
                <span class="table-title">${lead.leadName}</span>
                <span class="table-subtext">${lead.preferredLocation} · ${lead.brokerName}</span>
            </div>
            <div class="upcoming-meta">
                <strong>${formatDate(lead.followUpDate)}</strong>
                <span class="status-badge status-${lead.stage}">${formatEnumLabel(lead.stage)}</span>
            </div>
        </div>
    `).join("");
}

function renderLeadTable(leads) {
    const body = document.getElementById("leadTableBody");
    if (!body) {
        return;
    }
    if (!leads.length) {
        body.innerHTML = `<tr><td colspan="7" class="empty-state">No leads match the selected filters.</td></tr>`;
        return;
    }
    body.innerHTML = leads.map((lead) => `
        <tr>
            <td>
                <span class="table-title">${lead.name}</span>
                <span class="table-subtext">${lead.preferredLocation} · ${lead.source ? formatEnumLabel(lead.source) : "Unknown source"}</span>
            </td>
            <td>
                <span class="table-title">${formatBudgetRange(lead.budgetMin, lead.budgetMax)}</span>
                <span class="table-subtext">${lead.email}</span>
            </td>
            <td>${formatEnumLabel(lead.interestType)}</td>
            <td>${renderStatus(lead.stage)}</td>
            <td>${lead.agent?.name ?? "Unassigned"}</td>
            <td>${lead.followUpDate ? formatDate(lead.followUpDate) : "Not scheduled"}</td>
            <td>
                <div class="inline-actions">
                    <button class="button button-secondary" data-action="edit" data-id="${lead.id}">Edit</button>
                    <button class="button button-primary" data-action="convert" data-id="${lead.id}" ${lead.stage === "CONVERTED" ? "disabled" : ""}>Convert</button>
                    <button class="button button-danger" data-action="delete" data-id="${lead.id}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function renderRecentProperties(properties) {
    const body = document.getElementById("recentPropertiesBody");
    if (!body) {
        return;
    }
    if (!properties.length) {
        body.innerHTML = `<tr><td colspan="5" class="empty-state">No properties found.</td></tr>`;
        return;
    }
    body.innerHTML = properties.map((property) => `
        <tr>
            <td>
                <span class="table-title">${property.title}</span>
                <span class="table-subtext">${property.configuration} · ${formatArea(property.areaSqFt)}</span>
            </td>
            <td>${formatPropertyType(property.propertyType)}</td>
            <td>${property.locality}, ${property.city}</td>
            <td>${formatCurrency(property.price)}</td>
            <td>${renderStatus(property.status)}</td>
        </tr>
    `).join("");
}

function renderRecentBookings(bookings) {
    const body = document.getElementById("recentBookingsBody");
    if (!body) {
        return;
    }
    if (!bookings.length) {
        body.innerHTML = `<tr><td colspan="5" class="empty-state">No bookings found.</td></tr>`;
        return;
    }
    body.innerHTML = bookings.map((booking) => `
        <tr>
            <td>
                <span class="table-title">${booking.property.title}</span>
                <span class="table-subtext">${booking.property.location}</span>
            </td>
            <td>
                <span class="table-title">${booking.client.name}</span>
                <span class="table-subtext">${booking.client.phone}</span>
            </td>
            <td>${booking.agent.name}</td>
            <td>${formatDate(booking.bookingDate)}</td>
            <td>${renderStatus(booking.status)}</td>
        </tr>
    `).join("");
}

function renderPropertyTable(properties) {
    const body = document.getElementById("propertyTableBody");
    if (!body) {
        return;
    }
    if (!properties.length) {
        body.innerHTML = `<tr><td colspan="8" class="empty-state">No properties match the selected filters.</td></tr>`;
        return;
    }
    body.innerHTML = properties.map((property) => `
        <tr>
            <td>
                <span class="table-title">${property.title}</span>
                <span class="table-subtext">${property.featured ? "Featured listing" : "Standard listing"}</span>
            </td>
            <td>
                <span class="table-title">${property.locality}</span>
                <span class="table-subtext">${property.city}</span>
            </td>
            <td>
                <span class="table-title">${formatPropertyType(property.propertyType)}</span>
                <span class="table-subtext">${property.configuration} · ${formatArea(property.areaSqFt)}</span>
            </td>
            <td>${formatCurrency(property.price)}</td>
            <td>${renderStatus(property.status)}</td>
            <td>${property.agent?.name ?? "Unassigned"}</td>
            <td>${property.bookingCount}</td>
            <td>
                <div class="inline-actions">
                    <button class="button button-secondary" data-action="edit" data-id="${property.id}">Edit</button>
                    <button class="button button-danger" data-action="delete" data-id="${property.id}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function renderClientTable(clients) {
    const body = document.getElementById("clientTableBody");
    if (!body) {
        return;
    }
    if (!clients.length) {
        body.innerHTML = `<tr><td colspan="6" class="empty-state">No clients available.</td></tr>`;
        return;
    }
    body.innerHTML = clients.map((client) => `
        <tr>
            <td>
                <span class="table-title">${client.name}</span>
                <span class="table-subtext">Created ${client.createdAt ? formatDateTime(client.createdAt) : "recently"}</span>
            </td>
            <td>${client.email}</td>
            <td>${client.phone}</td>
            <td>${client.preferredLocation ?? "Not captured"}</td>
            <td>${client.bookingCount}</td>
            <td>
                <div class="inline-actions">
                    <button class="button button-secondary" data-action="edit" data-id="${client.id}">Edit</button>
                    <button class="button button-danger" data-action="delete" data-id="${client.id}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function renderAgentTable(agents) {
    const body = document.getElementById("agentTableBody");
    if (!body) {
        return;
    }
    if (!agents.length) {
        body.innerHTML = `<tr><td colspan="5" class="empty-state">No brokers available.</td></tr>`;
        return;
    }
    body.innerHTML = agents.map((agent) => `
        <tr>
            <td>
                <span class="table-title">${agent.name}</span>
                <span class="table-subtext">${agent.propertyCount} active listings · ${agent.leadCount ?? 0} leads</span>
            </td>
            <td>${agent.email}</td>
            <td>${agent.propertyCount}</td>
            <td>${agent.bookingCount}</td>
            <td>
                <div class="inline-actions">
                    <button class="button button-secondary" data-action="edit" data-id="${agent.id}">Edit</button>
                    <button class="button button-danger" data-action="delete" data-id="${agent.id}">Delete</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function renderBookingTable(bookings) {
    const body = document.getElementById("bookingTableBody");
    if (!body) {
        return;
    }
    if (!bookings.length) {
        body.innerHTML = `<tr><td colspan="7" class="empty-state">No bookings available.</td></tr>`;
        return;
    }
    const isAdmin = getUser()?.role === "ADMIN";
    body.innerHTML = bookings.map((booking) => `
        <tr>
            <td>
                <span class="table-title">${booking.property.title}</span>
                <span class="table-subtext">${booking.property.locality}, ${booking.property.city}</span>
            </td>
            <td>
                <span class="table-title">${booking.client.name}</span>
                <span class="table-subtext">${booking.client.preferredLocation ?? booking.client.email}</span>
            </td>
            <td>
                <span class="table-title">${booking.agent.name}</span>
                <span class="table-subtext">${booking.agent.email}</span>
            </td>
            <td>${formatDate(booking.bookingDate)}</td>
            <td>
                <span class="table-title">${formatCurrency(booking.amountPaid)} / ${formatCurrency(booking.bookingAmount)}</span>
                <span class="table-subtext">${formatEnumLabel(booking.paymentStatus)} · ${booking.paymentReference ?? "No reference"}</span>
            </td>
            <td>${renderStatus(booking.status)}</td>
            <td>
                <div class="inline-actions">
                    <button class="button button-secondary" data-action="edit" data-id="${booking.id}">Edit</button>
                    ${isAdmin ? `<button class="button button-danger" data-action="delete" data-id="${booking.id}">Delete</button>` : ""}
                </div>
            </td>
        </tr>
    `).join("");
}

async function handleLeadTableActions(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const lead = state.leads.items.find((item) => item.id === Number(button.dataset.id));
    if (!lead) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("leadId").value = lead.id;
        document.getElementById("leadName").value = lead.name;
        document.getElementById("leadEmail").value = lead.email;
        document.getElementById("leadPhone").value = lead.phone;
        document.getElementById("leadPreferredLocation").value = lead.preferredLocation;
        document.getElementById("leadBudgetMin").value = lead.budgetMin ?? "";
        document.getElementById("leadBudgetMax").value = lead.budgetMax ?? "";
        document.getElementById("leadInterestType").value = lead.interestType;
        document.getElementById("leadSource").value = lead.source;
        document.getElementById("leadFollowUpDate").value = lead.followUpDate ?? "";
        document.getElementById("leadStage").value = lead.stage;
        document.getElementById("leadNotes").value = lead.notes ?? "";
        document.getElementById("leadAgentId").value = lead.agent?.id ?? "";
        document.getElementById("cancelLeadEdit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
    }

    if (button.dataset.action === "convert") {
        const shouldConvert = await askConfirmation(
            "Convert lead to client?",
            `This will create a client profile for ${lead.name} and move the lead to Converted.`,
            "Convert"
        );
        if (!shouldConvert) {
            return;
        }
        try {
            await apiFetch(`/api/leads/${lead.id}/convert`, { method: "POST" });
            showToast("Lead converted to client");
            await Promise.all([loadLeads(), loadClientOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to convert lead", true);
        }
        return;
    }

    const shouldDelete = await askConfirmation(
        "Delete lead?",
        `This will permanently remove ${lead.name} from the pipeline.`,
        "Delete"
    );
    if (shouldDelete) {
        try {
            await apiFetch(`/api/leads/${lead.id}`, { method: "DELETE" });
            showToast("Lead deleted");
            await loadLeads();
        } catch (error) {
            showToast(error.message || "Failed to delete lead", true);
        }
    }
}

async function handlePropertyTableActions(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const property = state.properties.items.find((item) => item.id === Number(button.dataset.id));
    if (!property) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("propertyId").value = property.id;
        document.getElementById("propertyTitle").value = property.title;
        document.getElementById("propertyCity").value = property.city ?? "";
        document.getElementById("propertyLocality").value = property.locality ?? "";
        document.getElementById("propertyType").value = property.propertyType ?? "FLAT";
        document.getElementById("propertyConfiguration").value = property.configuration ?? "";
        document.getElementById("propertyAreaSqFt").value = property.areaSqFt ?? "";
        document.getElementById("propertyPrice").value = property.price;
        document.getElementById("propertyStatus").value = property.status;
        document.getElementById("propertyFeatured").checked = Boolean(property.featured);
        document.getElementById("propertyAgentId").value = property.agent?.id ?? "";
        document.getElementById("cancelPropertyEdit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
    }

    const shouldDelete = await askConfirmation(
        "Delete property?",
        `This will permanently remove "${property.title}" from inventory.`,
        "Delete"
    );
    if (shouldDelete) {
        try {
            await apiFetch(`/api/properties/${property.id}`, { method: "DELETE" });
            showToast("Property deleted");
            await Promise.all([loadProperties(), loadPropertyOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to delete property", true);
        }
    }
}

async function handleClientTableActions(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const client = state.clients.items.find((item) => item.id === Number(button.dataset.id));
    if (!client) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("clientId").value = client.id;
        document.getElementById("clientName").value = client.name;
        document.getElementById("clientEmail").value = client.email;
        document.getElementById("clientPhone").value = client.phone;
        document.getElementById("clientPreferredLocation").value = client.preferredLocation ?? "";
        document.getElementById("cancelClientEdit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
    }

    const shouldDelete = await askConfirmation(
        "Delete client?",
        `This will permanently remove ${client.name} from the client ledger.`,
        "Delete"
    );
    if (shouldDelete) {
        try {
            await apiFetch(`/api/clients/${client.id}`, { method: "DELETE" });
            showToast("Client deleted");
            await Promise.all([loadClients(), loadClientOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to delete client", true);
        }
    }
}

async function handleAgentTableActions(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const agent = state.agents.items.find((item) => item.id === Number(button.dataset.id));
    if (!agent) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("agentId").value = agent.id;
        document.getElementById("agentName").value = agent.name;
        document.getElementById("agentEmail").value = agent.email;
        document.getElementById("agentPassword").value = "";
        document.getElementById("cancelAgentEdit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
    }

    const shouldDelete = await askConfirmation(
        "Delete broker?",
        `This will permanently remove ${agent.name} from the broker roster.`,
        "Delete"
    );
    if (shouldDelete) {
        try {
            await apiFetch(`/api/agents/${agent.id}`, { method: "DELETE" });
            showToast("Broker deleted");
            await Promise.all([loadAgents(), loadAgentOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to delete broker", true);
        }
    }
}

async function handleBookingTableActions(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const booking = state.bookings.items.find((item) => item.id === Number(button.dataset.id));
    if (!booking) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("bookingId").value = booking.id;
        document.getElementById("bookingPropertyId").value = booking.property.id;
        document.getElementById("bookingClientId").value = booking.client.id;
        document.getElementById("bookingAgentId").value = booking.agent.id;
        document.getElementById("bookingDate").value = booking.bookingDate;
        document.getElementById("bookingAmount").value = booking.bookingAmount;
        document.getElementById("bookingAmountPaid").value = booking.amountPaid;
        document.getElementById("bookingStatus").value = booking.status;
        document.getElementById("bookingPaymentStatus").value = booking.paymentStatus ?? "NOT_STARTED";
        document.getElementById("bookingPaymentDate").value = booking.paymentDate ?? "";
        document.getElementById("bookingPaymentReference").value = booking.paymentReference ?? "";
        document.getElementById("cancelBookingEdit").classList.remove("hidden");
        window.scrollTo({ top: 0, behavior: "smooth" });
        return;
    }

    const shouldDelete = await askConfirmation(
        "Delete booking?",
        `This will remove booking #${booking.id} and resync the linked property status.`,
        "Delete"
    );
    if (shouldDelete) {
        try {
            await apiFetch(`/api/bookings/${booking.id}`, { method: "DELETE" });
            showToast("Booking deleted");
            await Promise.all([loadBookings(), loadPropertyOptions()]);
        } catch (error) {
            showToast(error.message || "Failed to delete booking", true);
        }
    }
}

function renderPagination(containerId, pageData, callback) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    container.innerHTML = `
        <button class="button button-secondary" ${pageData.first ? "disabled" : ""} data-page="${pageData.page - 1}">Previous</button>
        <span class="muted">Page ${pageData.page + 1} of ${Math.max(pageData.totalPages, 1)}</span>
        <button class="button button-secondary" ${pageData.last ? "disabled" : ""} data-page="${pageData.page + 1}">Next</button>
    `;
    container.querySelectorAll("button[data-page]").forEach((button) => {
        button.addEventListener("click", () => callback(Number(button.dataset.page)));
    });
}

function renderClientInsights(clients) {
    const visibleCount = document.getElementById("clientsVisibleCount");
    const bookingSum = document.getElementById("clientsBookingSum");
    if (visibleCount) {
        visibleCount.textContent = clients.length;
    }
    if (bookingSum) {
        bookingSum.textContent = clients.reduce((sum, client) => sum + (client.bookingCount || 0), 0);
    }
}

function renderLeadInsights(leads) {
    const visibleCount = document.getElementById("leadsVisibleCount");
    const hotCount = document.getElementById("leadsHotCount");
    const averageBudget = document.getElementById("leadsAverageBudget");
    const dueCount = document.getElementById("leadsDueCount");
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (visibleCount) {
        visibleCount.textContent = leads.length;
    }
    if (hotCount) {
        hotCount.textContent = leads.filter((lead) => ["SITE_VISIT_SCHEDULED", "NEGOTIATION"].includes(lead.stage)).length;
    }
    if (averageBudget) {
        const midpoints = leads
            .filter((lead) => lead.budgetMin && lead.budgetMax)
            .map((lead) => (Number(lead.budgetMin) + Number(lead.budgetMax)) / 2);
        const average = midpoints.length ? midpoints.reduce((sum, value) => sum + value, 0) / midpoints.length : 0;
        averageBudget.textContent = formatCurrency(average);
    }
    if (dueCount) {
        dueCount.textContent = leads.filter((lead) => {
            if (!lead.followUpDate) {
                return false;
            }
            const followUpDate = new Date(lead.followUpDate);
            followUpDate.setHours(0, 0, 0, 0);
            return followUpDate <= today && !["CONVERTED", "LOST"].includes(lead.stage);
        }).length;
    }
}

function renderAgentInsights(agents) {
    const visibleCount = document.getElementById("brokerVisibleCount");
    const propertySum = document.getElementById("brokerPropertySum");
    const bookingSum = document.getElementById("brokerBookingSum");
    const averageListings = document.getElementById("brokerAverageListings");
    if (visibleCount) {
        visibleCount.textContent = agents.length;
    }
    if (propertySum) {
        propertySum.textContent = agents.reduce((sum, agent) => sum + (agent.propertyCount || 0), 0);
    }
    if (bookingSum) {
        bookingSum.textContent = agents.reduce((sum, agent) => sum + (agent.bookingCount || 0), 0);
    }
    if (averageListings) {
        const totalListings = agents.reduce((sum, agent) => sum + (agent.propertyCount || 0), 0);
        averageListings.textContent = agents.length ? (totalListings / agents.length).toFixed(1) : "0";
    }
}

function renderBookingInsights(bookings) {
    const visibleCount = document.getElementById("bookingVisibleCount");
    const confirmedCount = document.getElementById("bookingConfirmedCount");
    const pendingCount = document.getElementById("bookingPendingCount");
    const upcomingCount = document.getElementById("bookingUpcomingCount");
    const collectedAmount = document.getElementById("bookingCollectedAmount");
    const outstandingAmount = document.getElementById("bookingOutstandingAmount");
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (visibleCount) {
        visibleCount.textContent = bookings.length;
    }
    if (confirmedCount) {
        confirmedCount.textContent = bookings.filter((booking) => booking.status === "CONFIRMED").length;
    }
    if (pendingCount) {
        pendingCount.textContent = bookings.filter((booking) => booking.status === "PENDING").length;
    }
    if (upcomingCount) {
        upcomingCount.textContent = bookings.filter((booking) => {
            const bookingDate = new Date(booking.bookingDate);
            bookingDate.setHours(0, 0, 0, 0);
            return bookingDate >= today;
        }).length;
    }
    if (collectedAmount) {
        collectedAmount.textContent = formatCurrency(bookings.reduce((sum, booking) => sum + Number(booking.amountPaid || 0), 0));
    }
    if (outstandingAmount) {
        outstandingAmount.textContent = formatCurrency(bookings.reduce((sum, booking) => sum + Number(booking.balanceAmount || 0), 0));
    }
}

function fillOptions(selectId, items, placeholder, includeBlank = false, labelBuilder = (item) => item.name) {
    const select = document.getElementById(selectId);
    if (!select) {
        return;
    }
    const placeholderOption = includeBlank
        ? `<option value="">${placeholder}</option>`
        : `<option value="" disabled selected>${placeholder}</option>`;
    select.innerHTML = placeholderOption + items.map((item) => `
        <option value="${item.id}">${labelBuilder(item)}</option>
    `).join("");
}

function renderStatus(status) {
    return `<span class="status-badge status-${status}">${formatEnumLabel(status)}</span>`;
}

function ensureSharedUi() {
    if (document.getElementById("confirmationModal")) {
        return;
    }

    const modal = document.createElement("div");
    modal.id = "confirmationModal";
    modal.className = "modal-overlay hidden";
    modal.innerHTML = `
        <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirmationTitle">
            <div class="modal-copy">
                <p class="eyebrow">Confirm Action</p>
                <h2 id="confirmationTitle">Please confirm</h2>
                <p id="confirmationMessage" class="copy"></p>
            </div>
            <div class="modal-actions">
                <button id="confirmationCancel" type="button" class="button button-secondary">Cancel</button>
                <button id="confirmationAccept" type="button" class="button button-danger">Delete</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);

    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            resolveConfirmation(false);
        }
    });
    document.getElementById("confirmationCancel").addEventListener("click", () => resolveConfirmation(false));
    document.getElementById("confirmationAccept").addEventListener("click", () => resolveConfirmation(true));
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !modal.classList.contains("hidden")) {
            resolveConfirmation(false);
        }
    });
}

function askConfirmation(title, message, actionLabel = "Confirm") {
    ensureSharedUi();
    const modal = document.getElementById("confirmationModal");
    document.getElementById("confirmationTitle").textContent = title;
    document.getElementById("confirmationMessage").textContent = message;
    document.getElementById("confirmationAccept").textContent = actionLabel;
    modal.classList.remove("hidden");
    document.body.classList.add("modal-open");

    return new Promise((resolve) => {
        askConfirmation.pendingResolve = resolve;
    });
}

function resolveConfirmation(value) {
    const modal = document.getElementById("confirmationModal");
    if (!modal || modal.classList.contains("hidden")) {
        return;
    }
    modal.classList.add("hidden");
    document.body.classList.remove("modal-open");
    if (askConfirmation.pendingResolve) {
        const pendingResolve = askConfirmation.pendingResolve;
        askConfirmation.pendingResolve = null;
        pendingResolve(value);
    }
}

function resetPropertyForm() {
    document.getElementById("propertyForm").reset();
    document.getElementById("propertyId").value = "";
    document.getElementById("cancelPropertyEdit").classList.add("hidden");
}

function resetLeadForm() {
    document.getElementById("leadForm").reset();
    document.getElementById("leadId").value = "";
    document.getElementById("cancelLeadEdit").classList.add("hidden");
}

function resetClientForm() {
    document.getElementById("clientForm").reset();
    document.getElementById("clientId").value = "";
    document.getElementById("cancelClientEdit").classList.add("hidden");
}

function resetAgentForm() {
    document.getElementById("agentForm").reset();
    document.getElementById("agentId").value = "";
    document.getElementById("cancelAgentEdit").classList.add("hidden");
}

function resetBookingForm() {
    document.getElementById("bookingForm").reset();
    document.getElementById("bookingId").value = "";
    document.getElementById("cancelBookingEdit").classList.add("hidden");
}

async function apiFetch(url, options = {}, withAuth = true) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };
    if (withAuth && getToken()) {
        headers.Authorization = `Bearer ${getToken()}`;
    }
    if (options.body === undefined) {
        delete headers["Content-Type"];
    }

    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
        let message = "Request failed";
        try {
            const data = await response.json();
            message = data.message || data.error || message;
        } catch (error) {
            message = response.statusText || message;
        }
        if (response.status === 401) {
            logout();
        }
        throw new Error(message);
    }
    if (response.status === 204) {
        return null;
    }
    const contentType = response.headers.get("Content-Type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return response.blob();
}

async function downloadFile(url, filename) {
    try {
        const blob = await apiFetch(url, { method: "GET" });
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = objectUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(objectUrl);
        showToast(`Downloaded ${filename}`);
    } catch (error) {
        showToast(error.message || "Download failed", true);
    }
}

function setAuth(authResponse) {
    localStorage.setItem(STORAGE_TOKEN_KEY, authResponse.token);
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify({
        userId: authResponse.userId,
        name: authResponse.name,
        email: authResponse.email,
        role: authResponse.role,
        brokerId: authResponse.brokerId ?? authResponse.agentId ?? null,
        agentId: authResponse.agentId
    }));
}

function getToken() {
    return localStorage.getItem(STORAGE_TOKEN_KEY);
}

function getUser() {
    const raw = localStorage.getItem(STORAGE_USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

function logout() {
    localStorage.removeItem(STORAGE_TOKEN_KEY);
    localStorage.removeItem(STORAGE_USER_KEY);
    window.location.href = "/";
}

function showToast(message, isError = false) {
    const toast = document.getElementById("toast");
    if (!toast) {
        return;
    }
    toast.textContent = message;
    toast.style.background = isError ? "rgba(166, 58, 58, 0.94)" : "rgba(38, 23, 13, 0.92)";
    toast.classList.add("show");
    window.clearTimeout(showToast.timeout);
    showToast.timeout = window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function addIfPresent(params, key, value) {
    if (value !== undefined && value !== null && value !== "") {
        params.append(key, value);
    }
}

function valueOrNull(value) {
    return value === "" ? null : Number(value);
}

function numberOrNull(value) {
    if (value === undefined || value === null || String(value).trim() === "") {
        return null;
    }
    const parsedValue = Number(value);
    return Number.isNaN(parsedValue) ? null : parsedValue;
}

function blankToNull(value) {
    return value.trim() === "" ? null : value.trim();
}

function formatCurrency(value) {
    const amount = Number(value);
    if (Number.isNaN(amount)) {
        return "Rs 0";
    }
    if (amount >= 10000000) {
        return `Rs ${(amount / 10000000).toFixed(amount >= 100000000 ? 0 : 2).replace(/\.00$/, "")} Cr`;
    }
    if (amount >= 100000) {
        return `Rs ${(amount / 100000).toFixed(amount >= 1000000 ? 0 : 2).replace(/\.00$/, "")} Lakh`;
    }
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    }).format(amount);
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric"
    }).format(new Date(value));
}

function formatDateTime(value) {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function formatRole(role) {
    if (role === "ADMIN") {
        return "Admin";
    }
    if (role === "AGENT") {
        return "Broker";
    }
    return role;
}

function formatPropertyType(propertyType) {
    if (!propertyType) {
        return "Flat";
    }
    if (propertyType === "COMMERCIAL") {
        return "Commercial";
    }
    return propertyType.charAt(0) + propertyType.slice(1).toLowerCase();
}

function formatArea(areaSqFt) {
    if (!areaSqFt) {
        return "Area pending";
    }
    return `${new Intl.NumberFormat("en-IN").format(areaSqFt)} sq ft`;
}

function formatBudgetRange(min, max) {
    if (min && max) {
        return `${formatCurrency(min)} - ${formatCurrency(max)}`;
    }
    if (min) {
        return `From ${formatCurrency(min)}`;
    }
    if (max) {
        return `Up to ${formatCurrency(max)}`;
    }
    return "Budget not captured";
}

function formatEnumLabel(value) {
    if (!value) {
        return "";
    }
    return value
        .toLowerCase()
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
}
