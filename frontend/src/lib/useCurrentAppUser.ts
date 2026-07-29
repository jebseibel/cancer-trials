import { useQuery } from '@tanstack/react-query';
import { appUserApi, authHelpers } from '../services/api';

// Login (User) and personal-tracking (AppUser) are separate tables with no FK between
// them. Match by username - each login account is expected to have a same-named AppUser
// row seeded for it.
export function useCurrentAppUser() {
    const username = authHelpers.getUsername();

    const query = useQuery({
        queryKey: ['appuser', 'current', username],
        queryFn: async () => {
            const response = await appUserApi.getAll();
            return response.data.content.find((u) => u.username === username) ?? null;
        },
        enabled: !!username,
    });

    return query;
}
