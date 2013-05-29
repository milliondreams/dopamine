'use strict';

describe('Service: socketService', function () {

  // load the service's module
  beforeEach(module('uiApp'));

  // instantiate service
  var socketService;
  beforeEach(inject(function (_socketService_) {
    socketService = _socketService_;
  }));

  it('should do something', function () {
    expect(!!socketService).toBe(true);
  });

});
