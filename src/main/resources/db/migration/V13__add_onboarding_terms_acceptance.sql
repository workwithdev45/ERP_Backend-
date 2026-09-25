-- G11: no Terms/Privacy acceptance was recorded at signup.
ALTER TABLE company_onboarding
    ADD COLUMN terms_accepted_at TIMESTAMPTZ,
    ADD COLUMN terms_version     VARCHAR(20);
