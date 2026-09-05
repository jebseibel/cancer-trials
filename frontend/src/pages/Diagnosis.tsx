import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Loader2 } from 'lucide-react';
import { patientApi, patientDiagnosisApi } from '../services/api';
import { BooleanSelect, Field, Section, Select, inputClass } from '../components/FormControls';
import { useCurrentPatient } from '../lib/PatientContext';
import { ageFromDateOfBirth, deriveReceptorSubtype } from '../lib/receptorSubtype';
import {
    ECOG_VALUES,
    MENOPAUSAL_STATUS_VALUES,
    RECEPTOR_STATUS_VALUES,
    SEX_VALUES,
    STAGE_SYSTEM_VALUES,
    STAGE_VALUES,
} from '../types/api';
import type { PatientDiagnosis, PatientDiagnosisRequest } from '../types/api';

// The form keeps every field as a string - that is what inputs give back - and converts on
// save. Keeping a mixed string/number/boolean draft in state means every handler has to know
// which kind it is touching.
type FormState = Record<string, string>;

const EMPTY_FORM: FormState = {
    cancerType: '',
    stage: '',
    stageSystem: '',
    isMetastatic: '',
    metastasisSites: '',
    erStatus: '',
    prStatus: '',
    her2Status: '',
    biomarkers: '',
    ecogStatus: '',
    priorChemoRegimens: '',
    lastChemoEndDate: '',
    priorTreatments: '',
    hasMeasurableDisease: '',
    menopausalStatus: '',
    dateOfBirth: '',
    sex: '',
    diagnosisDate: '',
    notes: '',
};

function toForm(d: PatientDiagnosis): FormState {
    const str = (v: string | number | boolean | undefined) =>
        v === undefined || v === null ? '' : String(v);
    return {
        cancerType: str(d.cancerType),
        stage: str(d.stage),
        stageSystem: str(d.stageSystem),
        isMetastatic: str(d.isMetastatic),
        metastasisSites: str(d.metastasisSites),
        erStatus: str(d.erStatus),
        prStatus: str(d.prStatus),
        her2Status: str(d.her2Status),
        biomarkers: str(d.biomarkers),
        ecogStatus: str(d.ecogStatus),
        priorChemoRegimens: str(d.priorChemoRegimens),
        lastChemoEndDate: str(d.lastChemoEndDate),
        priorTreatments: str(d.priorTreatments),
        hasMeasurableDisease: str(d.hasMeasurableDisease),
        menopausalStatus: str(d.menopausalStatus),
        diagnosisDate: str(d.diagnosisDate),
        notes: str(d.notes),
    };
}

/**
 * Blank inputs become null, never "". An empty string in a date field fails to parse
 * server-side, and for the text fields null is what the schema's clean_empty_strings()
 * would have produced anyway.
 */
function toRequest(form: FormState, patientExtid: string | undefined): PatientDiagnosisRequest {
    const text = (v: string) => (v.trim() === '' ? undefined : v.trim());
    const num = (v: string) => (v === '' ? undefined : Number(v));
    const bool = (v: string) => (v === '' ? undefined : v === 'true');

    return {
        patientExtid,
        cancerType: form.cancerType.trim(),
        stage: text(form.stage),
        stageSystem: text(form.stageSystem),
        isMetastatic: bool(form.isMetastatic),
        metastasisSites: text(form.metastasisSites),
        receptorSubtype:
            deriveReceptorSubtype(form.erStatus, form.prStatus, form.her2Status) ?? undefined,
        erStatus: text(form.erStatus),
        prStatus: text(form.prStatus),
        her2Status: text(form.her2Status),
        biomarkers: text(form.biomarkers),
        ecogStatus: num(form.ecogStatus),
        priorChemoRegimens: num(form.priorChemoRegimens),
        lastChemoEndDate: text(form.lastChemoEndDate),
        priorTreatments: text(form.priorTreatments),
        hasMeasurableDisease: bool(form.hasMeasurableDisease),
        menopausalStatus: text(form.menopausalStatus),
        diagnosisDate: text(form.diagnosisDate),
        notes: text(form.notes),
    };
}

export default function Diagnosis() {
    const queryClient = useQueryClient();
    const { patient, isLoading: patientLoading } = useCurrentPatient();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [saved, setSaved] = useState(false);

    // The queryFn returns the raw array so this cache entry stays shape-compatible with the
    // same-keyed query PatientRecord.tsx runs for the summary line and the record download -
    // `select` derives this page's single-row view rather than the queryFn itself returning a
    // different shape, which previously let whichever query ran last silently overwrite the
    // other's cached shape (an object here, an array there) with no error, just a wrong read.
    const { data: existing, isLoading } = useQuery({
        queryKey: ['patientDiagnosis', patient?.extid],
        queryFn: async () => (await patientDiagnosisApi.getByPatientExtid(patient!.extid)).data,
        select: (rows) => rows[0] ?? null,
        enabled: !!patient?.extid,
    });

    // Populate the form once the existing record arrives. Deliberately keyed on extid alone,
    // not the whole object: a refetch returning the same row must not overwrite edits in
    // progress, which is exactly what depending on `existing` would do.
    useEffect(() => {
        if (existing) setForm(toForm(existing));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [existing?.extid]);

    // Date of birth and sex describe the person, not the diagnosis, so they live on `patient`
    // and are loaded and saved separately - the two Save buttons on this page write to two
    // different tables, which is why they are not merged into one request.
    useEffect(() => {
        if (!patient) return;
        setForm((f) => ({
            ...f,
            dateOfBirth: patient.dateOfBirth ?? '',
            sex: patient.sex ?? '',
        }));
    }, [patient]);

    const saveMutation = useMutation({
        mutationFn: async () => {
            // Two tables, two writes. Date of birth and sex belong to the person and go to
            // `patient`; everything else is the diagnosis. The patient write goes first so a
            // failure there does not leave the person fields silently unsaved behind a
            // successful-looking diagnosis save.
            if (patient && (form.dateOfBirth !== (patient.dateOfBirth ?? '')
                    || form.sex !== (patient.sex ?? ''))) {
                await patientApi.update(patient.extid, {
                    dateOfBirth: form.dateOfBirth || undefined,
                    sex: form.sex || undefined,
                });
                await queryClient.invalidateQueries({ queryKey: ['patients', 'mine'] });
            }

            const request = toRequest(form, patient?.extid);
            return existing
                ? (await patientDiagnosisApi.update(existing.extid, request)).data
                : (await patientDiagnosisApi.create(request)).data;
        },
        onSuccess: async () => {
            setSaved(true);
            await queryClient.invalidateQueries({ queryKey: ['patientDiagnosis', patient?.extid] });
        },
    });

    const set = (field: string) => (value: string) => {
        setForm((prev) => ({ ...prev, [field]: value }));
        setSaved(false);
    };

    const derivedSubtype = deriveReceptorSubtype(form.erStatus, form.prStatus, form.her2Status);
    const age = ageFromDateOfBirth(form.dateOfBirth);
    const saveError = saveMutation.error as { response?: { data?: { message?: string } } } | null;

    if (patientLoading || isLoading) {
        return <p className="px-4 py-6 text-stone-500">Loading diagnosis...</p>;
    }

    if (!patient) {
        return (
            <div>
                <div className="bg-brand-beige-card shadow rounded-lg p-6">
                    <p className="text-base text-stone-700 leading-normal">
                        You don't have a patient record yet — create one and you can start
                        recording a diagnosis whenever you're ready.
                    </p>
                    <p className="mt-2 text-base text-stone-500 leading-normal">
                        A record can be for you or for someone you are helping.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div>
            <p className="text-base text-stone-500 leading-normal mb-6">
                Used to match against trial eligibility criteria. Everything here is optional
                except cancer type — record what you know, leave the rest blank.
            </p>

            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    saveMutation.mutate();
                }}
            >
                <Section title="Diagnosis">
                    <Field label="Cancer type" required className="sm:col-span-2">
                        <input
                            type="text"
                            required
                            maxLength={255}
                            value={form.cancerType}
                            onChange={(e) => set('cancerType')(e.target.value)}
                            placeholder="e.g. invasive ductal carcinoma of the breast"
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Diagnosis date">
                        <input
                            type="date"
                            value={form.diagnosisDate}
                            onChange={(e) => set('diagnosisDate')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Stage">
                        <Select
                            value={form.stage}
                            onChange={set('stage')}
                            options={STAGE_VALUES}
                        />
                    </Field>
                    <Field label="Staging system" hint="Editions differ — 7th and 8th both appear in trial criteria.">
                        <Select
                            value={form.stageSystem}
                            onChange={set('stageSystem')}
                            options={STAGE_SYSTEM_VALUES}
                        />
                    </Field>
                    <Field label="Metastatic">
                        <BooleanSelect value={form.isMetastatic} onChange={set('isMetastatic')} />
                    </Field>
                    <Field label="Metastasis sites" className="sm:col-span-2">
                        <input
                            type="text"
                            maxLength={500}
                            value={form.metastasisSites}
                            onChange={(e) => set('metastasisSites')(e.target.value)}
                            placeholder="comma-separated, e.g. bone, liver, brain"
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Receptors & biomarkers">
                    <Field label="ER status">
                        <Select
                            value={form.erStatus}
                            onChange={set('erStatus')}
                            options={RECEPTOR_STATUS_VALUES}
                        />
                    </Field>
                    <Field label="PR status">
                        <Select
                            value={form.prStatus}
                            onChange={set('prStatus')}
                            options={RECEPTOR_STATUS_VALUES}
                        />
                    </Field>
                    <Field label="HER2 status">
                        <Select
                            value={form.her2Status}
                            onChange={set('her2Status')}
                            options={RECEPTOR_STATUS_VALUES}
                        />
                    </Field>
                    <Field
                        label="Receptor subtype"
                        hint="Derived from the three receptors above, so the two can never disagree."
                        className="sm:col-span-3"
                    >
                        <div className="px-3 py-2 rounded-md bg-stone-50 border border-stone-200 text-base leading-normal">
                            {derivedSubtype ? (
                                <span className="font-medium text-stone-900">
                                    {derivedSubtype.replaceAll('_', ' ')}
                                </span>
                            ) : (
                                <span className="text-stone-500">
                                    Set ER, PR, and HER2 to positive or negative to derive this.
                                </span>
                            )}
                        </div>
                    </Field>
                    <Field label="Biomarkers" className="sm:col-span-3">
                        <input
                            type="text"
                            maxLength={1000}
                            value={form.biomarkers}
                            onChange={(e) => set('biomarkers')(e.target.value)}
                            placeholder="free text, e.g. BRCA1 germline, PD-L1 CPS 10, PIK3CA"
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Treatment history">
                    <Field label="Prior chemo regimens" hint="A count — criteria say “not more than 3 prior regimens”.">
                        <input
                            type="number"
                            min={0}
                            value={form.priorChemoRegimens}
                            onChange={(e) => set('priorChemoRegimens')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Last chemo end date" hint="Enables “within the past 12 months” windows.">
                        <input
                            type="date"
                            value={form.lastChemoEndDate}
                            onChange={(e) => set('lastChemoEndDate')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Measurable disease" hint="RECIST measurability.">
                        <BooleanSelect
                            value={form.hasMeasurableDisease}
                            onChange={set('hasMeasurableDisease')}
                        />
                    </Field>
                    <Field label="Prior treatments" className="sm:col-span-3">
                        <textarea
                            rows={3}
                            maxLength={2000}
                            value={form.priorTreatments}
                            onChange={(e) => set('priorTreatments')(e.target.value)}
                            placeholder="surgery, radiation, endocrine therapy, specific agents..."
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Patient">
                    <Field
                        label="Date of birth"
                        hint={age !== null ? `Age ${age} — derived, never stored.` : 'Age is derived from this.'}
                    >
                        <input
                            type="date"
                            value={form.dateOfBirth}
                            onChange={(e) => set('dateOfBirth')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Sex">
                        <Select value={form.sex} onChange={set('sex')} options={SEX_VALUES} />
                    </Field>
                    <Field label="ECOG performance status" hint="0–4. The one field trials compare numerically.">
                        <Select
                            value={form.ecogStatus}
                            onChange={set('ecogStatus')}
                            options={ECOG_VALUES.map(String)}
                        />
                    </Field>
                    <Field label="Menopausal status">
                        <Select
                            value={form.menopausalStatus}
                            onChange={set('menopausalStatus')}
                            options={MENOPAUSAL_STATUS_VALUES}
                        />
                    </Field>
                </Section>

                <Section title="Notes">
                    <Field
                        label="Clinical notes"
                        hint="Load-bearing: this is what gets compared when a criterion tests something the fields above don't model."
                        className="sm:col-span-3"
                    >
                        <textarea
                            rows={6}
                            value={form.notes}
                            onChange={(e) => set('notes')(e.target.value)}
                            placeholder="Anything the structured fields above can't capture..."
                            className={inputClass}
                        />
                    </Field>
                </Section>

                {saveMutation.isError && (
                    <div className="mb-4 rounded-md bg-red-50 border border-red-200 p-4 text-base leading-normal text-red-700">
                        {saveError?.response?.data?.message ?? 'Could not save the diagnosis.'}
                    </div>
                )}

                <div className="flex items-center gap-3">
                    <button
                        type="submit"
                        disabled={saveMutation.isPending}
                        className="inline-flex items-center px-4 py-2 rounded-md bg-brand-green text-white text-sm font-medium hover:bg-brand-green-hover disabled:opacity-50"
                    >
                        {saveMutation.isPending ? (
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        ) : (
                            <Save className="h-4 w-4 mr-2" />
                        )}
                        {saveMutation.isPending ? 'Saving...' : existing ? 'Save changes' : 'Save diagnosis'}
                    </button>
                    {saved && !saveMutation.isPending && (
                        <span className="text-sm text-brand-green-hover">Saved.</span>
                    )}
                </div>
            </form>
        </div>
    );
}

