import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Loader2 } from 'lucide-react';
import { patientPriorTreatmentApi } from '../services/api';
import { BooleanSelect, Field, Section, Select, inputClass } from '../components/FormControls';
import { useCurrentPatient } from '../lib/PatientContext';
import { TREATMENT_STATUS_LABELS, TREATMENT_STATUS_VALUES } from '../types/api';
import type { PatientPriorTreatment, PatientPriorTreatmentRequest } from '../types/api';

// Same convention as Diagnosis and Variants: every field is a string in state, converted on save.
type FormState = Record<string, string>;

const EMPTY_FORM: FormState = {
    cdk46Status: '',
    endocrineStatus: '',
    serdStatus: '',
    chemoStatus: '',
    her2TherapyStatus: '',
    her2AdcStatus: '',
    trop2AdcStatus: '',
    parpStatus: '',
    pi3kAktMtorStatus: '',
    immunotherapyStatus: '',
    taxaneStatus: '',
    anthracyclineStatus: '',
    platinumStatus: '',
    currentDrugNames: '',
    priorDrugNames: '',
    linesOfTherapyMetastatic: '',
    hadNeoadjuvant: '',
    hadAdjuvant: '',
    hadRadiation: '',
    hadSurgery: '',
    lastTreatmentEndDate: '',
    currentlyOnTreatment: '',
    otherTreatments: '',
    notes: '',
};

function toForm(t: PatientPriorTreatment): FormState {
    const str = (v: string | number | boolean | undefined) =>
        v === undefined || v === null ? '' : String(v);
    return {
        cdk46Status: str(t.cdk46Status),
        endocrineStatus: str(t.endocrineStatus),
        serdStatus: str(t.serdStatus),
        chemoStatus: str(t.chemoStatus),
        her2TherapyStatus: str(t.her2TherapyStatus),
        her2AdcStatus: str(t.her2AdcStatus),
        trop2AdcStatus: str(t.trop2AdcStatus),
        parpStatus: str(t.parpStatus),
        pi3kAktMtorStatus: str(t.pi3kAktMtorStatus),
        immunotherapyStatus: str(t.immunotherapyStatus),
        taxaneStatus: str(t.taxaneStatus),
        anthracyclineStatus: str(t.anthracyclineStatus),
        platinumStatus: str(t.platinumStatus),
        currentDrugNames: str(t.currentDrugNames),
        priorDrugNames: str(t.priorDrugNames),
        linesOfTherapyMetastatic: str(t.linesOfTherapyMetastatic),
        hadNeoadjuvant: str(t.hadNeoadjuvant),
        hadAdjuvant: str(t.hadAdjuvant),
        hadRadiation: str(t.hadRadiation),
        hadSurgery: str(t.hadSurgery),
        lastTreatmentEndDate: str(t.lastTreatmentEndDate),
        currentlyOnTreatment: str(t.currentlyOnTreatment),
        otherTreatments: str(t.otherTreatments),
        notes: str(t.notes),
    };
}

/** Blank inputs become undefined, never "" - an empty date string fails to parse server-side. */
function toRequest(
    form: FormState,
    patientExtid: string | undefined,
): PatientPriorTreatmentRequest {
    const text = (v: string) => (v.trim() === '' ? undefined : v.trim());
    const num = (v: string) => (v === '' ? undefined : Number(v));
    const bool = (v: string) => (v === '' ? undefined : v === 'true');

    return {
        patientExtid,
        cdk46Status: text(form.cdk46Status),
        endocrineStatus: text(form.endocrineStatus),
        serdStatus: text(form.serdStatus),
        chemoStatus: text(form.chemoStatus),
        her2TherapyStatus: text(form.her2TherapyStatus),
        her2AdcStatus: text(form.her2AdcStatus),
        trop2AdcStatus: text(form.trop2AdcStatus),
        parpStatus: text(form.parpStatus),
        pi3kAktMtorStatus: text(form.pi3kAktMtorStatus),
        immunotherapyStatus: text(form.immunotherapyStatus),
        taxaneStatus: text(form.taxaneStatus),
        anthracyclineStatus: text(form.anthracyclineStatus),
        platinumStatus: text(form.platinumStatus),
        currentDrugNames: text(form.currentDrugNames),
        priorDrugNames: text(form.priorDrugNames),
        linesOfTherapyMetastatic: num(form.linesOfTherapyMetastatic),
        hadNeoadjuvant: bool(form.hadNeoadjuvant),
        hadAdjuvant: bool(form.hadAdjuvant),
        hadRadiation: bool(form.hadRadiation),
        hadSurgery: bool(form.hadSurgery),
        lastTreatmentEndDate: text(form.lastTreatmentEndDate),
        currentlyOnTreatment: bool(form.currentlyOnTreatment),
        otherTreatments: text(form.otherTreatments),
        notes: text(form.notes),
    };
}

export default function PriorTreatment() {
    const queryClient = useQueryClient();
    const { patient, isLoading: patientLoading } = useCurrentPatient();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [saved, setSaved] = useState(false);

    const { data: existing, isLoading } = useQuery({
        queryKey: ['patientPriorTreatment', patient?.extid],
        queryFn: async () => {
            const rows = (await patientPriorTreatmentApi.getByPatientExtid(patient!.extid)).data;
            return rows[0] ?? null;
        },
        enabled: !!patient?.extid,
    });

    // Keyed on extid alone, not the whole object: a refetch returning the same row must not
    // overwrite edits in progress.
    useEffect(() => {
        if (existing) setForm(toForm(existing));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [existing?.extid]);

    const saveMutation = useMutation({
        mutationFn: async () => {
            const request = toRequest(form, patient?.extid);
            return existing
                ? (await patientPriorTreatmentApi.update(existing.extid, request)).data
                : (await patientPriorTreatmentApi.create(request)).data;
        },
        onSuccess: async () => {
            setSaved(true);
            await queryClient.invalidateQueries({
                queryKey: ['patientPriorTreatment', patient?.extid],
            });
        },
    });

    const set = (field: string) => (value: string) => {
        setForm((prev) => ({ ...prev, [field]: value }));
        setSaved(false);
    };

    const saveError = saveMutation.error as { response?: { data?: { message?: string } } } | null;

    if (patientLoading || isLoading) {
        return <p className="px-4 py-6 text-gray-500">Loading prior treatment...</p>;
    }

    if (!patient) {
        return (
            <div>
                <div className="bg-white shadow rounded-lg p-6">
                    <p className="text-sm text-gray-700">
                        No patient record yet. Create one to start recording past treatments.
                    </p>
                    <p className="mt-2 text-xs text-gray-500">
                        A record can be for you or for someone you are helping.
                    </p>
                </div>
            </div>
        );
    }

    const drug = (field: string, label: string, hint?: string) => (
        <Field label={label} hint={hint}>
            <Select
                value={form[field]}
                onChange={set(field)}
                options={TREATMENT_STATUS_VALUES}
                labels={TREATMENT_STATUS_LABELS}
            />
        </Field>
    );

    return (
        <div>
            <p className="text-sm text-gray-500 mb-4">
                Used to match against trial eligibility criteria. Everything here is optional —
                record what you know, leave the rest blank.
            </p>

            {/* Why these are dropdowns and not checkboxes. This is the distinction that decides
                which half of the corpus a patient is eligible for. */}
            <div className="mb-6 rounded-md border border-amber-300 bg-amber-50 px-4 py-3">
                <p className="text-sm text-amber-900">
                    <strong>How a drug was stopped matters as much as whether it was
                    taken.</strong>{' '}
                    Many trials split into two groups: people who have <em>never</em> had a drug
                    class, and people whose disease <em>progressed</em> on it. Someone taking a
                    drug right now belongs to neither. Choose <em>Stopped — it stopped
                    working</em> only when the disease progressed; use <em>Stopped — other
                    reason</em> for side effects or a completed course.
                </p>
            </div>

            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    saveMutation.mutate();
                }}
            >
                <Section title="Hormone-receptor therapies">
                    {drug(
                        'cdk46Status',
                        'CDK4/6 inhibitor',
                        'Palbociclib, ribociclib, abemaciclib (Verzenio). The single biggest eligibility gate.',
                    )}
                    {drug(
                        'endocrineStatus',
                        'Endocrine therapy',
                        'Letrozole, anastrozole, exemestane, tamoxifen, fulvestrant.',
                    )}
                    {drug('serdStatus', 'Oral SERD', 'Elacestrant, imlunestrant, vepdegestrant.')}
                    {drug(
                        'pi3kAktMtorStatus',
                        'PI3K / AKT / mTOR inhibitor',
                        'Alpelisib, inavolisib, capivasertib, everolimus.',
                    )}
                </Section>

                <Section title="HER2-targeted therapies">
                    {drug('her2TherapyStatus', 'HER2 antibody', 'Trastuzumab, pertuzumab.')}
                    {drug(
                        'her2AdcStatus',
                        'HER2 antibody-drug conjugate',
                        'Trastuzumab deruxtecan (Enhertu), T-DM1 (Kadcyla).',
                    )}
                    {drug(
                        'trop2AdcStatus',
                        'TROP2 antibody-drug conjugate',
                        'Sacituzumab govitecan, datopotamab deruxtecan.',
                    )}
                </Section>

                <Section title="Chemotherapy & other systemic therapy">
                    {drug('chemoStatus', 'Chemotherapy (any)')}
                    {drug('taxaneStatus', 'Taxane', 'Paclitaxel, docetaxel.')}
                    {drug('anthracyclineStatus', 'Anthracycline', 'Doxorubicin, epirubicin.')}
                    {drug('platinumStatus', 'Platinum', 'Carboplatin, cisplatin.')}
                    {drug('parpStatus', 'PARP inhibitor', 'Olaparib, talazoparib.')}
                    {drug(
                        'immunotherapyStatus',
                        'Immunotherapy',
                        'Pembrolizumab (Keytruda), atezolizumab.',
                    )}
                </Section>

                <Section title="Which drugs, specifically">
                    <Field
                        label="Taking now"
                        hint="Names as you know them - brand or generic both fine."
                        className="sm:col-span-3"
                    >
                        <input
                            type="text"
                            maxLength={1000}
                            value={form.currentDrugNames}
                            onChange={(e) => set('currentDrugNames')(e.target.value)}
                            placeholder="e.g. abemaciclib (Verzenio), letrozole"
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Taken previously" className="sm:col-span-3">
                        <input
                            type="text"
                            maxLength={1000}
                            value={form.priorDrugNames}
                            onChange={(e) => set('priorDrugNames')(e.target.value)}
                            placeholder="comma-separated"
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Treatment history">
                    <Field
                        label="Lines of therapy (metastatic)"
                        hint="0 means no treatment yet for metastatic disease - itself an inclusion criterion."
                    >
                        <input
                            type="number"
                            min={0}
                            value={form.linesOfTherapyMetastatic}
                            onChange={(e) => set('linesOfTherapyMetastatic')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Currently on treatment">
                        <BooleanSelect
                            value={form.currentlyOnTreatment}
                            onChange={set('currentlyOnTreatment')}
                        />
                    </Field>
                    <Field
                        label="Last treatment ended"
                        hint="Drives washout-window checks. Leave blank if still on treatment."
                    >
                        <input
                            type="date"
                            value={form.lastTreatmentEndDate}
                            onChange={(e) => set('lastTreatmentEndDate')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Had neoadjuvant therapy" hint="Before surgery.">
                        <BooleanSelect
                            value={form.hadNeoadjuvant}
                            onChange={set('hadNeoadjuvant')}
                        />
                    </Field>
                    <Field label="Had adjuvant therapy" hint="After surgery.">
                        <BooleanSelect value={form.hadAdjuvant} onChange={set('hadAdjuvant')} />
                    </Field>
                    <Field label="Had radiation">
                        <BooleanSelect value={form.hadRadiation} onChange={set('hadRadiation')} />
                    </Field>
                    <Field label="Had surgery">
                        <BooleanSelect value={form.hadSurgery} onChange={set('hadSurgery')} />
                    </Field>
                </Section>

                <Section title="Anything else">
                    <Field
                        label="Other treatments"
                        hint="Clinical trials, bone-directed therapy, anything not listed above."
                        className="sm:col-span-3"
                    >
                        <input
                            type="text"
                            maxLength={1000}
                            value={form.otherTreatments}
                            onChange={(e) => set('otherTreatments')(e.target.value)}
                            placeholder="e.g. denosumab, zoledronic acid"
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Notes" className="sm:col-span-3">
                        <textarea
                            rows={3}
                            value={form.notes}
                            onChange={(e) => set('notes')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                </Section>

                {saveMutation.isError && (
                    <div className="mb-4 rounded-md bg-red-50 px-4 py-3 text-sm text-red-700">
                        {saveError?.response?.data?.message ??
                            'Could not save the treatment history.'}
                    </div>
                )}

                <div className="flex items-center gap-3">
                    <button
                        type="submit"
                        disabled={saveMutation.isPending}
                        className="inline-flex items-center px-4 py-2 rounded-md bg-green-600 text-white text-sm font-medium hover:bg-green-700 disabled:opacity-50"
                    >
                        {saveMutation.isPending ? (
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        ) : (
                            <Save className="h-4 w-4 mr-2" />
                        )}
                        {saveMutation.isPending
                            ? 'Saving...'
                            : existing
                              ? 'Save changes'
                              : 'Save treatment history'}
                    </button>
                    {saved && !saveMutation.isPending && (
                        <span className="text-sm text-green-700">Saved.</span>
                    )}
                </div>
            </form>
        </div>
    );
}
