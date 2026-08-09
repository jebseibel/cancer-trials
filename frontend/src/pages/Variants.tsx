import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Dna, Save, Loader2 } from 'lucide-react';
import { patientVariantApi } from '../services/api';
import { Field, Section, Select, inputClass } from '../components/FormControls';
import { useCurrentAppUser } from '../lib/useCurrentAppUser';
import { VARIANT_STATUS_LABELS, VARIANT_STATUS_VALUES } from '../types/api';
import type { PatientVariant, PatientVariantRequest } from '../types/api';

// Same convention as Diagnosis: every field is a string in state, converted on save.
type FormState = Record<string, string>;

const EMPTY_FORM: FormState = {
    pik3caStatus: '',
    esr1Status: '',
    tp53Status: '',
    akt1Status: '',
    ptenStatus: '',
    erbb2SomaticStatus: '',
    brca1Status: '',
    brca2Status: '',
    palb2Status: '',
    atmStatus: '',
    chek2Status: '',
    hrdStatus: '',
    pdl1Status: '',
    ki67Percent: '',
    germlineTestDone: '',
    somaticTestDone: '',
    testDate: '',
    testLab: '',
    otherVariants: '',
    notes: '',
};

function toForm(v: PatientVariant): FormState {
    const str = (x: string | number | undefined) => (x === undefined || x === null ? '' : String(x));
    return {
        pik3caStatus: str(v.pik3caStatus),
        esr1Status: str(v.esr1Status),
        tp53Status: str(v.tp53Status),
        akt1Status: str(v.akt1Status),
        ptenStatus: str(v.ptenStatus),
        erbb2SomaticStatus: str(v.erbb2SomaticStatus),
        brca1Status: str(v.brca1Status),
        brca2Status: str(v.brca2Status),
        palb2Status: str(v.palb2Status),
        atmStatus: str(v.atmStatus),
        chek2Status: str(v.chek2Status),
        hrdStatus: str(v.hrdStatus),
        pdl1Status: str(v.pdl1Status),
        ki67Percent: str(v.ki67Percent),
        germlineTestDone: str(v.germlineTestDone),
        somaticTestDone: str(v.somaticTestDone),
        testDate: str(v.testDate),
        testLab: str(v.testLab),
        otherVariants: str(v.otherVariants),
        notes: str(v.notes),
    };
}

/** Blank inputs become undefined, never "" - an empty date string fails to parse server-side. */
function toRequest(form: FormState, appUserExtid: string | undefined): PatientVariantRequest {
    const text = (v: string) => (v.trim() === '' ? undefined : v.trim());
    const num = (v: string) => (v === '' ? undefined : Number(v));

    return {
        appUserExtid,
        pik3caStatus: text(form.pik3caStatus),
        esr1Status: text(form.esr1Status),
        tp53Status: text(form.tp53Status),
        akt1Status: text(form.akt1Status),
        ptenStatus: text(form.ptenStatus),
        erbb2SomaticStatus: text(form.erbb2SomaticStatus),
        brca1Status: text(form.brca1Status),
        brca2Status: text(form.brca2Status),
        palb2Status: text(form.palb2Status),
        atmStatus: text(form.atmStatus),
        chek2Status: text(form.chek2Status),
        hrdStatus: text(form.hrdStatus),
        pdl1Status: text(form.pdl1Status),
        ki67Percent: num(form.ki67Percent),
        germlineTestDone: text(form.germlineTestDone),
        somaticTestDone: text(form.somaticTestDone),
        testDate: text(form.testDate),
        testLab: text(form.testLab),
        otherVariants: text(form.otherVariants),
        notes: text(form.notes),
    };
}

export default function Variants() {
    const queryClient = useQueryClient();
    const { data: appUser, isLoading: appUserLoading } = useCurrentAppUser();
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [saved, setSaved] = useState(false);

    const { data: existing, isLoading } = useQuery({
        queryKey: ['patientVariant', appUser?.extid],
        queryFn: async () => {
            const rows = (await patientVariantApi.getByAppUserExtid(appUser!.extid)).data;
            return rows[0] ?? null;
        },
        enabled: !!appUser?.extid,
    });

    // Keyed on extid alone, not the whole object: a refetch returning the same row must not
    // overwrite edits in progress.
    useEffect(() => {
        if (existing) setForm(toForm(existing));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [existing?.extid]);

    const saveMutation = useMutation({
        mutationFn: async () => {
            const request = toRequest(form, appUser?.extid);
            return existing
                ? (await patientVariantApi.update(existing.extid, request)).data
                : (await patientVariantApi.create(request)).data;
        },
        onSuccess: async () => {
            setSaved(true);
            await queryClient.invalidateQueries({ queryKey: ['patientVariant', appUser?.extid] });
        },
    });

    const set = (field: string) => (value: string) => {
        setForm((prev) => ({ ...prev, [field]: value }));
        setSaved(false);
    };

    const saveError = saveMutation.error as { response?: { data?: { message?: string } } } | null;

    if (appUserLoading || isLoading) {
        return <p className="px-4 py-6 text-gray-500">Loading variants...</p>;
    }

    if (!appUser) {
        return (
            <div className="px-4 py-6 sm:px-0">
                <div className="bg-white shadow rounded-lg p-6">
                    <p className="text-sm text-gray-500">
                        No app-user profile linked to your login. Ask to have one seeded before
                        entering variants.
                    </p>
                </div>
            </div>
        );
    }

    const gene = (field: string, label: string, hint?: string) => (
        <Field label={label} hint={hint}>
            <Select
                value={form[field]}
                onChange={set(field)}
                options={VARIANT_STATUS_VALUES}
                labels={VARIANT_STATUS_LABELS}
            />
        </Field>
    );

    return (
        <div className="px-4 py-6 sm:px-0">
            <div className="flex items-center gap-2 mb-1">
                <Dna className="h-6 w-6 text-green-600" />
                <h1 className="text-2xl font-bold text-gray-900">Genomic & Germline Variants</h1>
            </div>
            <p className="text-sm text-gray-500 mb-4">
                Used to match against trial eligibility criteria. Everything here is optional —
                record what you know, leave the rest blank.
            </p>

            {/* The single most likely data-entry error, called out before the fields. */}
            <div className="mb-6 rounded-md border border-amber-300 bg-amber-50 px-4 py-3">
                <p className="text-sm text-amber-900">
                    <strong>&ldquo;Not tested&rdquo; is not the same as &ldquo;not
                    detected&rdquo;.</strong>{' '}
                    If a gene was never tested for, choose <em>Not tested</em> rather than leaving
                    it blank or marking it negative. A trial that requires a BRCA mutation is a
                    question worth asking about when the test was never done — but a genuine
                    mismatch when the test came back negative.
                </p>
            </div>

            <form
                onSubmit={(e) => {
                    e.preventDefault();
                    saveMutation.mutate();
                }}
            >
                <Section title="Was testing done?">
                    <Field
                        label="Tumor sequencing (somatic)"
                        hint="Foundation Medicine, Guardant, Tempus and similar."
                    >
                        <Select
                            value={form.somaticTestDone}
                            onChange={set('somaticTestDone')}
                            options={VARIANT_STATUS_VALUES}
                            labels={VARIANT_STATUS_LABELS}
                        />
                    </Field>
                    <Field label="Germline panel (inherited)" hint="Invitae, Myriad and similar.">
                        <Select
                            value={form.germlineTestDone}
                            onChange={set('germlineTestDone')}
                            options={VARIANT_STATUS_VALUES}
                            labels={VARIANT_STATUS_LABELS}
                        />
                    </Field>
                    <Field label="Test date">
                        <input
                            type="date"
                            value={form.testDate}
                            onChange={(e) => set('testDate')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                    <Field label="Testing lab" className="sm:col-span-2">
                        <input
                            type="text"
                            maxLength={255}
                            value={form.testLab}
                            onChange={(e) => set('testLab')(e.target.value)}
                            placeholder="e.g. Foundation Medicine"
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Tumor (somatic) findings">
                    {gene('pik3caStatus', 'PIK3CA', 'Gates PI3K/AKT inhibitor trials.')}
                    {gene('esr1Status', 'ESR1', 'Marks endocrine resistance; gates oral SERD trials.')}
                    {gene('akt1Status', 'AKT1')}
                    {gene('ptenStatus', 'PTEN')}
                    {gene('tp53Status', 'TP53')}
                    {gene(
                        'erbb2SomaticStatus',
                        'ERBB2 (somatic)',
                        'A tumor mutation — not the same as HER2 receptor status on the Diagnosis page.',
                    )}
                </Section>

                <Section title="Inherited (germline) findings">
                    {gene('brca1Status', 'BRCA1', 'Gates PARP inhibitor trials.')}
                    {gene('brca2Status', 'BRCA2', 'Gates PARP inhibitor trials.')}
                    {gene('palb2Status', 'PALB2')}
                    {gene('atmStatus', 'ATM')}
                    {gene('chek2Status', 'CHEK2')}
                    {gene('hrdStatus', 'HRD', 'Homologous recombination deficiency — a composite score.')}
                </Section>

                <Section title="Other biomarkers">
                    {gene('pdl1Status', 'PD-L1', 'Gates most immunotherapy trials.')}
                    <Field label="Ki-67 (%)" hint="Proliferation index. Leave blank if unknown.">
                        <input
                            type="number"
                            min={0}
                            max={100}
                            value={form.ki67Percent}
                            onChange={(e) => set('ki67Percent')(e.target.value)}
                            className={inputClass}
                        />
                    </Field>
                </Section>

                <Section title="Anything else">
                    <Field
                        label="Other variants"
                        hint="Anything not listed above, as written on the report."
                        className="sm:col-span-3"
                    >
                        <input
                            type="text"
                            maxLength={1000}
                            value={form.otherVariants}
                            onChange={(e) => set('otherVariants')(e.target.value)}
                            placeholder="e.g. FGFR1 amplification"
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
                        {saveError?.response?.data?.message ?? 'Could not save the variants.'}
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
                        {saveMutation.isPending ? 'Saving...' : existing ? 'Save changes' : 'Save variants'}
                    </button>
                    {saved && !saveMutation.isPending && (
                        <span className="text-sm text-green-700">Saved.</span>
                    )}
                </div>
            </form>
        </div>
    );
}
