import { CacheProcessor } from '../src/ts/cache/cache-processor';
import { UserActionDescription } from '../src/ts/cache/model';
import { Event, EventStatus } from '../src/ts/event/model-and-schema/model';
import { SignalDetection } from '../src/ts/signal-detection/model';

const contextBob = { sessionId: 'test1', userName: 'Uncle Bob' };
const contextMaria = { sessionId: 'test2', userName: 'Aunt Maria' };
const contextLetters = { sessionId: 'test3', userName: 'Letters' };
const contextRasco = { sessionId: 'test4', userName: 'Rasco' };

const mockSdBob: SignalDetection = {
  id: 'bob',
  monitoringOrganization: 'bob',
  stationName: 'bob',
  signalDetectionHypotheses: [],
  currentHypothesis: undefined,
  associations: [],
  hasConflict: false
};

const mockSdMaria: SignalDetection = {
  id: 'maria',
  monitoringOrganization: 'maria',
  stationName: 'maria',
  signalDetectionHypotheses: [],
  currentHypothesis: undefined,
  associations: [],
  hasConflict: false
};

const mockEventBob: Partial<Event> = {
  id: 'bob',
  monitoringOrganization: 'bob',
  preferredEventHypothesisHistory: [],
  finalEventHypothesisHistory: [],
  hypotheses: [],
  status: EventStatus.OpenForRefinement,
  currentEventHypothesis: {
    eventHypothesis: undefined,
    processingStageId: 'bob'
  },
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

const mockEventMaria: Partial<Event> = {
  id: 'maria',
  monitoringOrganization: 'maria',
  preferredEventHypothesisHistory: [],
  finalEventHypothesisHistory: [],
  hypotheses: [],
  status: EventStatus.OpenForRefinement,
  currentEventHypothesis: {
    eventHypothesis: undefined,
    processingStageId: 'maria'
  },
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

const cacheBob = CacheProcessor.Instance().getCacheForUser(contextBob.sessionId);
const cacheMaria = CacheProcessor.Instance().getCacheForUser(contextMaria.sessionId);

const cacheRascoEvent: Partial<Event> = {
  id: 'rasco',
  monitoringOrganization: 'rasco',
  preferredEventHypothesisHistory: [],
  finalEventHypothesisHistory: [],
  hypotheses: [],
  status: EventStatus.OpenForRefinement,
  currentEventHypothesis: {
    eventHypothesis: undefined,
    processingStageId: 'rasco'
  },
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

const sharedEvent: Partial<Event> = {
  id: 'shared-event-1',
  monitoringOrganization: 'shared-event-1',
  preferredEventHypothesisHistory: [],
  finalEventHypothesisHistory: [],
  hypotheses: [],
  status: EventStatus.OpenForRefinement,
  currentEventHypothesis: {
    eventHypothesis: undefined,
    processingStageId: 'shared-event-1'
  },
  associations: [],
  signalDetectionIds: [],
  hasConflict: false
};

const sharedSd: Partial<SignalDetection> = {
  id: 'shared-sd-1',
  monitoringOrganization: 'shared-sd-1',
  stationName: 'shared-sd-1',
  signalDetectionHypotheses: [],
  currentHypothesis: undefined,
  associations: [],
  hasConflict: false
};

CacheProcessor.Instance().addLoadedEventsToGlobalCache([sharedEvent as any]);
CacheProcessor.Instance().addLoadedSdsToGlobalCache([sharedSd as any]);

const cacheLetters = CacheProcessor.Instance().getCacheForUser(contextLetters.sessionId);
const cacheRasco = CacheProcessor.Instance().getCacheForUser(contextRasco.sessionId);

xdescribe('User Cache Access', () => {
  it('A users local cache is not affects by unsaved changes to another cache', () => {
    cacheBob.setSignalDetection(UserActionDescription.UPDATE_DETECTION, mockSdBob);
    cacheMaria.setSignalDetection(UserActionDescription.UPDATE_DETECTION, mockSdMaria);

    expect(cacheBob.getSignalDetectionById('a').stationName).toEqual('bob');
  });
  it('After a push, the other users cache is updated', () => {
    cacheBob.setEvent(UserActionDescription.UNKNOWN, mockEventBob as any);
    cacheMaria.setEvent(UserActionDescription.UNKNOWN, mockEventMaria as any);
    cacheMaria.commitAllEvents();

    expect(cacheBob.getEventById('a').monitoringOrganization).toEqual('maria');
  });
});

xdescribe('Publishing individual changes', () => {
  it('Saving one event does not override the users other events', () => {
    const changedSharedEvent: any = {
      id: 'sharedEvent',
      monitoringOrganization: 'changed'
    };
    const changedSharedEventTwo: any = {
      id: 'sharedEventTwo',
      monitoringOrganization: 'changed'
    };
    cacheLetters.setEvent(UserActionDescription.UNKNOWN, changedSharedEvent);
    cacheLetters.setEvent(UserActionDescription.UNKNOWN, changedSharedEventTwo);
    cacheLetters.commitEventsWithIds(['sharedEvent']);
    expect(cacheLetters.getEventById('sharedEventTwo').monitoringOrganization).toEqual('changed');
  });
  it('Saving one event correct populates the other user', () => {
    cacheRasco.setEvent(UserActionDescription.UNKNOWN, cacheRascoEvent as any);
    cacheRasco.commitEventsWithIds([cacheRascoEvent.id]);
    expect(cacheLetters.getEventById(cacheRascoEvent.id).monitoringOrganization).toEqual(
      cacheRasco.getEventById(cacheRascoEvent.id).monitoringOrganization
    );
  });
});

describe('Workspace State Test', () => {
  test('Adding event to users to workspace state', () => {
    let workspaceState = CacheProcessor.Instance().getWorkspaceState();
    expect(workspaceState).toBeDefined();
    expect(workspaceState).toMatchSnapshot();

    CacheProcessor.Instance().addOrUpdateEventToUser('test', 'user1');
    workspaceState = CacheProcessor.Instance().getWorkspaceState();
    expect(workspaceState).toBeDefined();
    expect(workspaceState).toMatchSnapshot();

    CacheProcessor.Instance().removeUserFromEvent('test', 'user1');
    workspaceState = CacheProcessor.Instance().getWorkspaceState();
    expect(workspaceState).toBeDefined();
    expect(workspaceState).toMatchSnapshot();
  });
});
